<?php

namespace App\Services;

use App\Models\Installation;
use App\Models\Reminder;
use Illuminate\Support\Carbon;

/**
 * Decides what should be said about an installation, and when.
 *
 * The rules, in full, so they are not scattered across the app:
 *
 *   service_due     customer   30 days before the service is due
 *   service_due     customer   on the day it is due
 *   warranty_ending customer   30 days before the manufacturer warranty ends
 *
 * A pending reminder is cancelled when the thing it was going to talk about has
 * already happened: the service was carried out, the customer asked for a booking,
 * or the installation was archived. Reminders are never created in the past, and a
 * reminder that has been sitting unfired for longer than STALE_AFTER_DAYS is skipped
 * rather than sent, so a week of downtime does not turn into a week of late messages
 * arriving at once.
 */
class ReminderPlanner
{
    public const LEAD_DAYS_SERVICE = 30;
    public const LEAD_DAYS_WARRANTY = 30;
    public const STALE_AFTER_DAYS = 14;

    /** The installer may bring the customer's service warning forward or push it back. */
    private function leadDays(Installation $installation): int
    {
        $lead = (int) ($installation->user?->reminder_lead_days ?? self::LEAD_DAYS_SERVICE);

        return max(7, min(84, $lead));
    }

    /**
     * Rebuild the pending reminders for one installation. Fired and cancelled reminders
     * are left untouched, so history is preserved.
     *
     * @return int number of reminders now pending
     */
    public function sync(Installation $installation, ?Carbon $today = null): int
    {
        $today = ($today ?? Carbon::today())->startOfDay();

        $installation->reminders()
            ->where('status', Reminder::STATUS_PENDING)
            ->update(['status' => Reminder::STATUS_CANCELLED, 'reason' => 'replanned']);

        if ($installation->status !== Installation::STATUS_ACTIVE) {
            return 0;
        }

        $slots = $this->slotsFor($installation, $today);

        foreach ($slots as $slot) {
            Reminder::updateOrCreate(
                [
                    'installation_id' => $installation->id,
                    'kind' => $slot['kind'],
                    'audience' => $slot['audience'],
                    'fire_on' => $slot['fire_on'],
                ],
                [
                    'user_id' => $installation->user_id,
                    'status' => Reminder::STATUS_PENDING,
                    'reason' => null,
                    'fired_at' => null,
                ]
            );
        }

        return count($slots);
    }

    /**
     * @return array<int, array{kind: string, audience: string, fire_on: Carbon}>
     */
    public function slotsFor(Installation $installation, Carbon $today): array
    {
        $slots = [];

        if ($due = $installation->next_service_due_on) {
            $due = $due->copy()->startOfDay();

            if ($due->lt($today)) {
                // The date has already passed. This happens on a unit recorded long after it
                // was fitted, or after a spell with the reminders switched off. Without this
                // the customer would never hear anything at all, because every other slot is
                // in the past: one catch-up goes out today, and only if nothing was said
                // about this cycle already.
                if (! $this->alreadyToldAbout($installation, $due)) {
                    $slots[] = ['kind' => Reminder::KIND_SERVICE_DUE, 'audience' => Reminder::AUDIENCE_CUSTOMER, 'fire_on' => $today->copy()];
                }
            } else {
                $early = $due->copy()->subDays($this->leadDays($installation));
                if ($early->gte($today)) {
                    $slots[] = ['kind' => Reminder::KIND_SERVICE_DUE, 'audience' => Reminder::AUDIENCE_CUSTOMER, 'fire_on' => $early];
                }

                $slots[] = ['kind' => Reminder::KIND_SERVICE_DUE, 'audience' => Reminder::AUDIENCE_CUSTOMER, 'fire_on' => $due];
            }
        }

        if ($expiry = $installation->warranty_expires_on) {
            $warn = $expiry->copy()->startOfDay()->subDays(self::LEAD_DAYS_WARRANTY);
            if ($warn->gte($today)) {
                $slots[] = ['kind' => Reminder::KIND_WARRANTY_ENDING, 'audience' => Reminder::AUDIENCE_CUSTOMER, 'fire_on' => $warn];
            }
        }

        return $slots;
    }

    /**
     * Has anything already been said to this customer about the cycle that ended on $due?
     * Used to make sure the catch-up message goes out once, not once per edit.
     */
    private function alreadyToldAbout(Installation $installation, Carbon $due): bool
    {
        return $installation->reminders()
            ->where('kind', Reminder::KIND_SERVICE_DUE)
            ->where('status', Reminder::STATUS_FIRED)
            ->whereDate('fire_on', '>=', $due->copy()->subDays(self::LEAD_DAYS_SERVICE + 1))
            ->exists();
    }

    /** The customer asked for a visit, so stop telling them to ask for a visit. */
    public function cancelServiceRemindersFor(Installation $installation, string $reason): int
    {
        return $installation->reminders()
            ->where('status', Reminder::STATUS_PENDING)
            ->where('kind', Reminder::KIND_SERVICE_DUE)
            ->update(['status' => Reminder::STATUS_CANCELLED, 'reason' => $reason]);
    }

    public function cancelAll(Installation $installation, string $reason): int
    {
        return $installation->reminders()
            ->where('status', Reminder::STATUS_PENDING)
            ->update(['status' => Reminder::STATUS_CANCELLED, 'reason' => $reason]);
    }

    public function isStale(Reminder $reminder, Carbon $today): bool
    {
        return $reminder->fire_on->copy()->addDays(self::STALE_AFTER_DAYS)->lt($today->copy()->startOfDay());
    }
}
