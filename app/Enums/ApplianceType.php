<?php

namespace App\Enums;

/**
 * The kinds of machine an installer fits, with the two numbers that matter:
 * how long the manufacturer warranty usually runs, and how often the unit
 * should be serviced. Both are only defaults — every install can override them,
 * because warranty terms differ per manufacturer and per market.
 */
enum ApplianceType: string
{
    case GasBoiler = 'gas_boiler';
    case CondensingBoiler = 'condensing_boiler';
    case SplitAc = 'split_ac';
    case MultiSplitAc = 'multi_split_ac';
    case HeatPump = 'heat_pump';
    case WaterHeater = 'water_heater';
    case SolarThermal = 'solar_thermal';
    case SolarPv = 'solar_pv';
    case AlarmSystem = 'alarm_system';
    case Ventilation = 'ventilation';
    case Other = 'other';

    /** Translation key for the human label. */
    public function labelKey(): string
    {
        return 'appliance.'.$this->value;
    }

    public function label(): string
    {
        return __($this->labelKey());
    }

    /**
     * The name of an icon in resources/icons.php, not a character.
     *
     * This used to return an emoji. That was a mistake: emoji are drawn by
     * whatever font the phone happens to carry, render at a size nobody chose,
     * and cannot take a colour from the design tokens — so the one glyph that
     * has to read as "gas" at arm's length could not be relied on to look like
     * anything in particular. A vector glyph does what it is told everywhere.
     */
    public function icon(): string
    {
        return match ($this) {
            self::GasBoiler, self::CondensingBoiler => 'flame',
            self::SplitAc, self::MultiSplitAc => 'snowflake',
            self::HeatPump => 'heat-pump',
            self::WaterHeater => 'shower',
            self::SolarThermal => 'sun',
            self::SolarPv => 'solar',
            self::AlarmSystem => 'bell',
            self::Ventilation => 'fan',
            self::Other => 'wrench',
        };
    }

    /** Typical manufacturer warranty length in months. A default, never a promise. */
    public function defaultWarrantyMonths(): int
    {
        return match ($this) {
            self::GasBoiler, self::CondensingBoiler => 24,
            self::SplitAc, self::MultiSplitAc => 24,
            self::HeatPump => 60,
            self::WaterHeater => 24,
            self::SolarThermal => 60,
            self::SolarPv => 120,
            self::AlarmSystem => 24,
            self::Ventilation => 24,
            self::Other => 24,
        };
    }

    /** How often this unit should be serviced, in months. */
    public function defaultServiceMonths(): int
    {
        return match ($this) {
            self::GasBoiler, self::CondensingBoiler => 12,
            self::SplitAc, self::MultiSplitAc => 12,
            self::HeatPump => 12,
            self::WaterHeater => 24,
            self::SolarThermal => 12,
            self::SolarPv => 12,
            self::AlarmSystem => 12,
            self::Ventilation => 12,
            self::Other => 12,
        };
    }

    /**
     * The month a service is genuinely most useful, so a due date can be nudged
     * to land before the season the unit is needed: heating before winter,
     * cooling before summer. Null means no seasonal preference.
     */
    public function seasonalMonth(): ?int
    {
        return match ($this) {
            self::GasBoiler, self::CondensingBoiler, self::SolarThermal => 10, // serviced before the heating season
            self::SplitAc, self::MultiSplitAc, self::Ventilation => 5,          // serviced before the cooling season
            default => null,
        };
    }

    /** Fields worth reading off this machine's data plate, as translation keys. */
    public function plateHints(): array
    {
        return match ($this) {
            self::GasBoiler, self::CondensingBoiler => ['brand', 'model', 'serial', 'gc_number', 'kw'],
            self::SplitAc, self::MultiSplitAc, self::HeatPump => ['brand', 'model', 'serial', 'kw', 'refrigerant'],
            self::SolarPv => ['brand', 'model', 'serial', 'kw'],
            default => ['brand', 'model', 'serial'],
        };
    }

    /** @return array<int, self> */
    public static function ordered(): array
    {
        return [
            self::GasBoiler,
            self::SplitAc,
            self::CondensingBoiler,
            self::MultiSplitAc,
            self::WaterHeater,
            self::HeatPump,
            self::SolarPv,
            self::SolarThermal,
            self::AlarmSystem,
            self::Ventilation,
            self::Other,
        ];
    }

    /** @return array<string, string> */
    public static function options(): array
    {
        $out = [];
        foreach (self::ordered() as $case) {
            $out[$case->value] = $case->label();
        }

        return $out;
    }
}
