<?php

namespace App\Http\Requests;

use App\Enums\ApplianceType;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class UpdateInstallationRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    public function rules(): array
    {
        return [
            'appliance_type' => ['required', Rule::in(array_column(ApplianceType::cases(), 'value'))],
            'brand' => ['nullable', 'string', 'max:120'],
            'model' => ['nullable', 'string', 'max:120'],
            'serial' => ['nullable', 'string', 'max:120'],
            'extra_code' => ['nullable', 'string', 'max:120'],
            'capacity' => ['nullable', 'string', 'max:60'],
            'refrigerant' => ['nullable', 'string', 'max:40'],
            'plate_photo' => ['nullable', 'image', 'max:'.config('installbook.uploads.max_kb')],
            'installed_on' => ['required', 'date', 'before_or_equal:today'],
            'warranty_months' => ['required', 'integer', 'between:0,240'],
            'service_interval_months' => ['required', 'integer', 'between:0,120'],
            'service_required_for_warranty' => ['nullable', 'boolean'],
            'location_note' => ['nullable', 'string', 'max:160'],
            'notes' => ['nullable', 'string', 'max:2000'],
        ];
    }

    protected function prepareForValidation(): void
    {
        $this->merge([
            'service_required_for_warranty' => $this->boolean('service_required_for_warranty'),
        ]);
    }

    public function validated($key = null, $default = null): array
    {
        $data = parent::validated();
        unset($data['plate_photo']);

        return $data;
    }
}
