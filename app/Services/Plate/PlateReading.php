<?php

namespace App\Services\Plate;

use Illuminate\Contracts\Support\Arrayable;

/**
 * What was read off a data plate. Every field is optional, because plates are
 * photographed in basements at the end of long days and half of them are unreadable.
 */
class PlateReading implements Arrayable
{
    public function __construct(
        public readonly ?string $brand = null,
        public readonly ?string $model = null,
        public readonly ?string $serial = null,
        public readonly ?string $extraCode = null,
        public readonly ?string $capacity = null,
        public readonly ?int $manufacturedYear = null,
        public readonly ?string $rawText = null,
        public readonly ?string $error = null,
        public readonly ?float $confidence = null,
    ) {}

    public static function empty(?string $error = null): self
    {
        return new self(error: $error);
    }

    public function isEmpty(): bool
    {
        return $this->brand === null && $this->model === null && $this->serial === null;
    }

    public function toArray(): array
    {
        return array_filter([
            'brand' => $this->brand,
            'model' => $this->model,
            'serial' => $this->serial,
            'extra_code' => $this->extraCode,
            'capacity' => $this->capacity,
            'manufactured_year' => $this->manufacturedYear,
            'raw_text' => $this->rawText,
            'error' => $this->error,
            'confidence' => $this->confidence,
        ], fn ($v) => $v !== null);
    }
}
