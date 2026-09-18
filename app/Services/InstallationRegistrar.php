<?php

namespace App\Services;

use App\Enums\ApplianceType;
use App\Models\Customer;
use App\Models\Installation;
use App\Models\ServiceVisit;
use App\Models\User;
use App\Services\Messaging\MessageDispatcher;
use App\Support\Phone;
use Illuminate\Support\Carbon;
use Illuminate\Support\Facades\DB;

/**
 * Recording one install: find or create the customer, store the machine, log the first
 * visit, work out the two dates, plan the reminders and write the card message.
 *
 * It is one transaction because a half-recorded install is worse than none: the whole
 * value of the register is that the installer can trust what is in it.
 */
class InstallationRegistrar
{
    public function __construct(
        private readonly ScheduleCalculator $schedule,
        private readonly ReminderPlanner $reminders,
        private readonly MessageDispatcher $dispatcher,
    ) {}

    /**
     * @param  array{
     *   phone: string, customer_name?: ?string, address?: ?string, city?: ?string,
     *   lat?: ?float, lng?: ?float, customer_locale?: ?string, messaging_consent?: bool,
     *   appliance_type: string, brand?: ?string, model?: ?string, serial?: ?string,
     *   extra_code?: ?string, capacity?: ?string, plate_photo_path?: ?string,
     *   unit_photo_path?: ?string, plate_reading?: ?array, installed_on?: ?string,
     *   warranty_months?: ?int, service_interval_months?: ?int,
     *   service_required_for_warranty?: bool, location_note?: ?string, notes?: ?string,
     *   send_card?: bool
     * }  $data
     */
    public function register(User $user, array $data): Installation
    {
        $type = ApplianceType::tryFrom($data['appliance_type']) ?? ApplianceType::Other;
        $installedOn = isset($data['installed_on'])
            ? Carbon::parse($data['installed_on'])->startOfDay()
            : Carbon::today();

        $warrantyMonths = (int) ($data['warranty_months'] ?? $type->defaultWarrantyMonths());
        $serviceMonths = (int) ($data['service_interval_months'] ?? $type->defaultServiceMonths());

        $installation = DB::transaction(function () use ($user, $data, $type, $installedOn, $warrantyMonths, $serviceMonths) {
            $customer = $this->customer($user, $data);

            $installation = $user->installations()->create([
                'customer_id' => $customer?->id,
                'appliance_type' => $type->value,
                'brand' => $data['brand'] ?? null,
                'model' => $data['model'] ?? null,
                'serial' => $data['serial'] ?? null,
                'extra_code' => $data['extra_code'] ?? null,
                'capacity' => $data['capacity'] ?? null,
                'plate_photo_path' => $data['plate_photo_path'] ?? null,
                'unit_photo_path' => $data['unit_photo_path'] ?? null,
                'plate_reading' => $data['plate_reading'] ?? null,
                'installed_on' => $installedOn,
                'warranty_months' => $warrantyMonths,
                'warranty_expires_on' => $this->schedule->warrantyExpiry($installedOn, $warrantyMonths),
                'registration_deadline_on' => $this->schedule->registrationDeadline(
                    $installedOn,
                    (int) ($user->registration_window_days ?? 30),
                ),
                'refrigerant' => $data['refrigerant'] ?? null,
                'service_interval_months' => $serviceMonths,
                'next_service_due_on' => $this->schedule->nextServiceDue($type, $installedOn, $serviceMonths),
                'service_required_for_warranty' => (bool) ($data['service_required_for_warranty'] ?? false),
                'location_note' => $data['location_note'] ?? null,
                'notes' => $data['notes'] ?? null,
                'status' => Installation::STATUS_ACTIVE,
            ]);

            $installation->visits()->create([
                'user_id' => $user->id,
                'kind' => ServiceVisit::KIND_INSTALL,
                'performed_on' => $installedOn,
                'notes' => $data['notes'] ?? null,
                'photo_path' => $data['unit_photo_path'] ?? null,
            ]);

            $this->reminders->sync($installation);

            return $installation;
        });

        if (($data['send_card'] ?? true) && $installation->customer_id) {
            $this->dispatcher->sendCard($installation->fresh(['customer', 'user']));
        }

        return $installation->fresh(['customer', 'visits']);
    }

    /**
     * Recording a service on a unit: the visit is logged, the clock restarts from the
     * visit date, and the reminders are replanned for the next cycle.
     */
    public function recordVisit(Installation $installation, array $data): ServiceVisit
    {
        return DB::transaction(function () use ($installation, $data) {
            $performedOn = isset($data['performed_on'])
                ? Carbon::parse($data['performed_on'])->startOfDay()
                : Carbon::today();

            $visit = $installation->visits()->create([
                'user_id' => $installation->user_id,
                'kind' => $data['kind'] ?? ServiceVisit::KIND_SERVICE,
                'performed_on' => $performedOn,
                'notes' => $data['notes'] ?? null,
                'photo_path' => $data['photo_path'] ?? null,
                'price_cents' => isset($data['price']) && $data['price'] !== null && $data['price'] !== ''
                    ? (int) round(((float) $data['price']) * 100)
                    : null,
                'currency' => $data['currency'] ?? config('installbook.defaults.currency'),
            ]);

            // Only a service restarts the service clock; a repair or an inspection does not.
            if ($visit->kind === ServiceVisit::KIND_SERVICE) {
                $installation->next_service_due_on = $this->schedule->nextServiceDue(
                    $installation->type(),
                    $performedOn,
                    $installation->service_interval_months,
                );
                $installation->save();
                $this->reminders->sync($installation);
            }

            return $visit;
        });
    }

    /** Recompute both dates after the installer edits an install. */
    public function refreshSchedule(Installation $installation): Installation
    {
        $lastService = $installation->visits()
            ->where('kind', ServiceVisit::KIND_SERVICE)
            ->orderByDesc('performed_on')
            ->first();

        $from = $lastService?->performed_on ?? $installation->installed_on;

        $installation->warranty_expires_on = $this->schedule->warrantyExpiry(
            $installation->installed_on,
            $installation->warranty_months,
        );
        $installation->next_service_due_on = $this->schedule->nextServiceDue(
            $installation->type(),
            $from,
            $installation->service_interval_months,
        );
        $installation->save();

        $this->reminders->sync($installation);

        return $installation;
    }

    /** Null when no phone number was given: the unit is recorded without a customer. */
    private function customer(User $user, array $data): ?Customer
    {
        $phone = Phone::normalize($data['phone'] ?? null, config('installbook.defaults.country_prefix'));

        if ($phone === null) {
            return null;
        }

        $customer = $user->customers()->where('phone', $phone)->first();

        $attributes = array_filter([
            'name' => $data['customer_name'] ?? null,
            'address' => $data['address'] ?? null,
            'city' => $data['city'] ?? null,
            'lat' => $data['lat'] ?? null,
            'lng' => $data['lng'] ?? null,
            'locale' => $data['customer_locale'] ?? null,
        ], fn ($v) => $v !== null && $v !== '');

        $consented = (bool) ($data['messaging_consent'] ?? false);

        if ($customer) {
            // Never blank out what is already known just because a field was left empty.
            $customer->fill($attributes);
            if ($consented && ! $customer->messaging_consent_at) {
                $customer->messaging_consent_at = now();
                $customer->consent_text = $data['consent_text'] ?? __('consent.statement', ['business' => $user->displayName()], $customer->locale);
                $customer->consent_method = 'in_person_at_install';
                $customer->opted_out_at = null;
            }
            $customer->save();

            return $customer;
        }

        $locale = $data['customer_locale'] ?? $user->locale;

        return $user->customers()->create($attributes + [
            'phone' => $phone,
            'locale' => $locale,
            'messaging_consent_at' => $consented ? now() : null,
            'consent_text' => $consented
                ? ($data['consent_text'] ?? __('consent.statement', ['business' => $user->displayName()], $locale))
                : null,
            'consent_method' => $consented ? 'in_person_at_install' : null,
        ]);
    }
}
