<?php

namespace App\Services\Plate;

/**
 * The default. No credentials, no network call, no cost: the installer photographs the
 * plate for the record and types the three fields that matter.
 */
class NullPlateReader implements PlateReader
{
    public function read(string $absolutePath, ?string $applianceType = null): PlateReading
    {
        return PlateReading::empty();
    }

    public function isEnabled(): bool
    {
        return false;
    }
}
