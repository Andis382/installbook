<?php

namespace App\Services;

use App\Enums\ApplianceType;
use Illuminate\Support\Carbon;

/**
 * Turns an install date plus two intervals into the only two dates the product cares
 * about: when the warranty ends and when the unit is next due a service.
 *
 * The one piece of judgement here is seasonality. A boiler installed in March is
 * technically due again in March, but the service that is actually worth doing happens
 * before the heating season. Where an appliance has a season, the due date is nudged
 * towards it, but never by more than MAX_SEASONAL_SHIFT_MONTHS, so the interval the
 * manufacturer asked for is still roughly honoured.
 */
class ScheduleCalculator
{
    public const MAX_SEASONAL_SHIFT_MONTHS = 3;

    public function warrantyExpiry(Carbon $installedOn, int $warrantyMonths): ?Carbon
    {
        if ($warrantyMonths <= 0) {
            return null;
        }

        return $installedOn->copy()->startOfDay()->addMonthsNoOverflow($warrantyMonths);
    }

    /**
     * The next service date after $from (defaults to the install date).
     */
    /**
     * The date by which the unit has to be registered with the manufacturer for the
     * commercial warranty to start at all. Usually a short window after installation.
     */
    public function registrationDeadline(Carbon $installedOn, int $windowDays): ?Carbon
    {
        if ($windowDays <= 0) {
            return null;
        }

        return $installedOn->copy()->startOfDay()->addDays($windowDays);
    }

    public function nextServiceDue(
        ApplianceType $type,
        Carbon $from,
        int $intervalMonths,
        bool $applySeason = true
    ): ?Carbon {
        if ($intervalMonths <= 0) {
            return null;
        }

        $naive = $from->copy()->startOfDay()->addMonthsNoOverflow($intervalMonths);

        if (! $applySeason) {
            return $naive;
        }

        return $this->applySeason($naive, $type->seasonalMonth());
    }

    /**
     * Move a due date to the appliance's useful month when that month is close enough
     * to the calculated date. Returns the original date when there is no season or the
     * shift would be too big.
     */
    public function applySeason(Carbon $naive, ?int $seasonalMonth): Carbon
    {
        if ($seasonalMonth === null) {
            return $naive;
        }

        $candidates = [
            Carbon::create($naive->year - 1, $seasonalMonth, 1)->startOfDay(),
            Carbon::create($naive->year, $seasonalMonth, 1)->startOfDay(),
            Carbon::create($naive->year + 1, $seasonalMonth, 1)->startOfDay(),
        ];

        $best = null;
        $bestDistance = null;

        foreach ($candidates as $candidate) {
            $distance = abs($naive->diffInDays($candidate));
            if ($bestDistance === null || $distance < $bestDistance) {
                $best = $candidate;
                $bestDistance = $distance;
            }
        }

        if ($best === null || $bestDistance > self::MAX_SEASONAL_SHIFT_MONTHS * 31) {
            return $naive;
        }

        // Keep the original day of the month where the month has one.
        $day = min($naive->day, $best->daysInMonth);

        return $best->copy()->setDay($day);
    }

    /**
     * True when the due date was moved for seasonal reasons, so the interface can say so
     * instead of looking like it got the arithmetic wrong.
     */
    public function wasMovedForSeason(ApplianceType $type, Carbon $from, int $intervalMonths, Carbon $actual): bool
    {
        $naive = $this->nextServiceDue($type, $from, $intervalMonths, applySeason: false);

        return $naive !== null && ! $naive->isSameDay($actual);
    }
}
