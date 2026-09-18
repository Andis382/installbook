<?php

namespace Tests\Unit;

use App\Enums\ApplianceType;
use App\Services\ScheduleCalculator;
use Illuminate\Support\Carbon;
use PHPUnit\Framework\TestCase;

class ScheduleCalculatorTest extends TestCase
{
    private ScheduleCalculator $calc;

    protected function setUp(): void
    {
        parent::setUp();
        $this->calc = new ScheduleCalculator;
    }

    public function test_warranty_expiry_is_install_date_plus_months(): void
    {
        $expiry = $this->calc->warrantyExpiry(Carbon::parse('2026-03-15'), 24);

        $this->assertSame('2028-03-15', $expiry->toDateString());
    }

    public function test_warranty_expiry_is_null_without_a_warranty(): void
    {
        $this->assertNull($this->calc->warrantyExpiry(Carbon::parse('2026-03-15'), 0));
    }

    public function test_end_of_month_install_does_not_overflow_into_the_next_month(): void
    {
        // 31 January plus one month must be 28/29 February, not 2 or 3 March.
        $expiry = $this->calc->warrantyExpiry(Carbon::parse('2026-01-31'), 1);

        $this->assertSame('2026-02-28', $expiry->toDateString());
    }

    public function test_registration_deadline_is_install_date_plus_window(): void
    {
        $deadline = $this->calc->registrationDeadline(Carbon::parse('2026-03-15'), 30);

        $this->assertSame('2026-04-14', $deadline->toDateString());
        $this->assertNull($this->calc->registrationDeadline(Carbon::parse('2026-03-15'), 0));
    }

    public function test_a_unit_without_a_season_is_due_exactly_one_interval_later(): void
    {
        $due = $this->calc->nextServiceDue(ApplianceType::WaterHeater, Carbon::parse('2026-03-15'), 24);

        $this->assertSame('2028-03-15', $due->toDateString());
    }

    public function test_a_boiler_due_near_winter_is_pulled_back_to_before_the_heating_season(): void
    {
        // Installed in December, so the plain date is next December, in the middle of the
        // season. October is two months earlier, inside the bound, so it moves.
        $due = $this->calc->nextServiceDue(ApplianceType::GasBoiler, Carbon::parse('2026-12-20'), 12);

        $this->assertSame(10, $due->month, 'a boiler should be serviced before the heating season');
        $this->assertSame(2027, $due->year);
    }

    public function test_an_air_conditioner_due_in_early_spring_is_nudged_to_before_summer(): void
    {
        $due = $this->calc->nextServiceDue(ApplianceType::SplitAc, Carbon::parse('2026-03-12'), 12);

        $this->assertSame(5, $due->month, 'an air conditioner should be serviced before summer');
        $this->assertSame(2027, $due->year);
    }

    /**
     * The nudge is a convenience, never a licence to move a manufacturer's interval by half
     * a year. A boiler fitted in May stays due in May: October is five months away, which is
     * outside the bound, and quietly servicing five months late could breach the warranty.
     */
    public function test_a_date_far_from_the_useful_season_is_left_alone(): void
    {
        $due = $this->calc->nextServiceDue(ApplianceType::GasBoiler, Carbon::parse('2026-05-10'), 12);

        $this->assertSame('2027-05-10', $due->toDateString());
    }

    public function test_the_seasonal_shift_is_bounded(): void
    {
        // A boiler already due in October must not be moved at all.
        $due = $this->calc->nextServiceDue(ApplianceType::GasBoiler, Carbon::parse('2026-10-08'), 12);

        $this->assertSame('2027-10-08', $due->toDateString());
    }

    public function test_the_shift_never_exceeds_the_declared_maximum(): void
    {
        foreach ([1, 3, 5, 7, 9, 11] as $month) {
            $from = Carbon::create(2026, $month, 12);
            $naive = $this->calc->nextServiceDue(ApplianceType::GasBoiler, $from, 12, applySeason: false);
            $actual = $this->calc->nextServiceDue(ApplianceType::GasBoiler, $from, 12);

            $this->assertLessThanOrEqual(
                ScheduleCalculator::MAX_SEASONAL_SHIFT_MONTHS * 31,
                abs($naive->diffInDays($actual)),
                "shift too large for month {$month}"
            );
        }
    }

    public function test_every_appliance_type_has_usable_defaults(): void
    {
        foreach (ApplianceType::cases() as $type) {
            $this->assertGreaterThan(0, $type->defaultWarrantyMonths(), $type->value);
            $this->assertGreaterThan(0, $type->defaultServiceMonths(), $type->value);
            $season = $type->seasonalMonth();
            if ($season !== null) {
                $this->assertGreaterThanOrEqual(1, $season);
                $this->assertLessThanOrEqual(12, $season);
            }
        }
    }
}
