<?php

namespace App\Services\Plate;

interface PlateReader
{
    /**
     * Read what is printed on a photographed data plate.
     *
     * Implementations must never throw: a failed read returns an empty PlateReading and
     * the installer types the fields, which is the path that always works.
     */
    public function read(string $absolutePath, ?string $applianceType = null): PlateReading;

    public function isEnabled(): bool;
}
