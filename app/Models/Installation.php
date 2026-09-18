<?php

namespace App\Models;

use App\Enums\ApplianceType;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;
use Illuminate\Support\Carbon;
use Illuminate\Support\Str;

class Installation extends Model
{
    use HasFactory;

    public const STATUS_ACTIVE = 'active';
    public const STATUS_ARCHIVED = 'archived';

    protected $fillable = [
        'user_id', 'customer_id', 'appliance_type', 'brand', 'model', 'serial',
        'serial_key', 'extra_code', 'capacity', 'refrigerant', 'plate_photo_path', 'unit_photo_path',
        'plate_reading', 'installed_on', 'warranty_months', 'warranty_expires_on',
        'registration_deadline_on', 'manufacturer_registered_at',
        'service_interval_months', 'next_service_due_on', 'service_required_for_warranty',
        'location_note', 'notes', 'public_token', 'status',
    ];

    protected function casts(): array
    {
        return [
            'installed_on' => 'date',
            'warranty_expires_on' => 'date',
            'registration_deadline_on' => 'date',
            'manufacturer_registered_at' => 'datetime',
            'next_service_due_on' => 'date',
            'service_required_for_warranty' => 'boolean',
            'plate_reading' => 'array',
        ];
    }

    protected static function booted(): void
    {
        static::creating(function (Installation $installation) {
            $installation->public_token ??= self::newToken();
        });

        // The search key is derived, never typed, so it is kept in step here rather than
        // at every place a serial can be set.
        static::saving(function (Installation $installation) {
            if ($installation->isDirty('serial')) {
                $installation->serial_key = \App\Support\Serial::key($installation->serial);
            }
        });
    }

    public static function newToken(): string
    {
        return Str::lower(Str::random(24));
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function customer(): BelongsTo
    {
        return $this->belongsTo(Customer::class);
    }

    public function visits(): HasMany
    {
        return $this->hasMany(ServiceVisit::class)->orderByDesc('performed_on');
    }

    public function plates(): HasMany
    {
        return $this->hasMany(InstallationPlate::class);
    }

    public function reminders(): HasMany
    {
        return $this->hasMany(Reminder::class);
    }

    public function messages(): HasMany
    {
        return $this->hasMany(Message::class);
    }

    public function bookingRequests(): HasMany
    {
        return $this->hasMany(BookingRequest::class);
    }

    public function type(): ApplianceType
    {
        return ApplianceType::tryFrom($this->appliance_type) ?? ApplianceType::Other;
    }

    public function typeLabel(): string
    {
        return $this->type()->label();
    }

    /** "Vaillant ecoTEC plus" or whatever of it is known. */
    public function unitName(): string
    {
        $parts = array_filter([$this->brand, $this->model]);

        return $parts ? implode(' ', $parts) : $this->typeLabel();
    }

    /**
     * The guarantee the buyer has by law, independently of the manufacturer. In the EU it
     * is at least two years from delivery, it is owed by the seller, and nothing the
     * customer does or fails to do can void it. It is computed rather than stored because
     * it is a property of the law, not of this install, and it must never be presented as
     * being at risk.
     */
    public function statutoryGuaranteeUntil(): ?Carbon
    {
        $months = (int) config('installbook.defaults.statutory_warranty_months', 24);

        if ($months <= 0 || ! $this->installed_on) {
            return null;
        }

        return $this->installed_on->copy()->addMonthsNoOverflow($months);
    }

    /** True while the manufacturer registration window is open and nobody has registered. */
    public function needsManufacturerRegistration(?Carbon $on = null): bool
    {
        return $this->manufacturer_registered_at === null
            && $this->registration_deadline_on !== null
            && $this->status === self::STATUS_ACTIVE;
    }

    public function registrationDaysLeft(?Carbon $on = null): ?int
    {
        if (! $this->registration_deadline_on) {
            return null;
        }

        return (int) ($on ?? Carbon::today())->startOfDay()->diffInDays($this->registration_deadline_on, false);
    }

    public function warrantyIsActive(?Carbon $on = null): bool
    {
        if (! $this->warranty_expires_on) {
            return false;
        }

        return $this->warranty_expires_on->gte(($on ?? Carbon::today())->startOfDay());
    }

    public function warrantyDaysLeft(?Carbon $on = null): ?int
    {
        if (! $this->warranty_expires_on) {
            return null;
        }

        return (int) ($on ?? Carbon::today())->startOfDay()->diffInDays($this->warranty_expires_on, false);
    }

    public function serviceDaysAway(?Carbon $on = null): ?int
    {
        if (! $this->next_service_due_on) {
            return null;
        }

        return (int) ($on ?? Carbon::today())->startOfDay()->diffInDays($this->next_service_due_on, false);
    }

    public function isOverdue(?Carbon $on = null): bool
    {
        $days = $this->serviceDaysAway($on);

        return $days !== null && $days < 0;
    }

    public function lastServiceVisit(): ?ServiceVisit
    {
        return $this->visits->firstWhere(fn (ServiceVisit $v) => $v->kind === ServiceVisit::KIND_SERVICE);
    }

    public function scopeActive($query)
    {
        return $query->where('status', self::STATUS_ACTIVE);
    }

    public function scopeDueBy($query, Carbon $date)
    {
        return $query->whereNotNull('next_service_due_on')->whereDate('next_service_due_on', '<=', $date);
    }

    /**
     * Units whose manufacturer registration window is still open and unused. This is a live
     * query rather than a queued reminder: it is the installer's own to-do list, it is only
     * ever shown inside the app, and it must be right the moment he opens it.
     */
    public function scopeAwaitingRegistration($query)
    {
        return $query->where('status', self::STATUS_ACTIVE)
            ->whereNull('manufacturer_registered_at')
            ->whereNotNull('registration_deadline_on');
    }
}
