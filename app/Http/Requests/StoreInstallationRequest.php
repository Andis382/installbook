<?php

namespace App\Http\Requests;

use App\Enums\ApplianceType;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class StoreInstallationRequest extends FormRequest
{
    public function authorize(): bool
    {
        return $this->user() !== null;
    }

    public function rules(): array
    {
        $maxKb = config('installbook.uploads.max_kb');

        return [
            // Nothing about the customer is required. A phone number is what the card is
            // sent to and how the same person is recognised on the next job, so the form
            // asks for it first and clearly — but a save is never refused for missing it.
            // The record with a photo and no name is still worth more than the note that
            // was never written because the app said no.
            'phone' => ['nullable', 'string', 'max:32'],
            'customer_name' => ['nullable', 'string', 'max:120'],
            'address' => ['nullable', 'string', 'max:200'],
            'city' => ['nullable', 'string', 'max:120'],
            'lat' => ['nullable', 'numeric', 'between:-90,90'],
            'lng' => ['nullable', 'numeric', 'between:-180,180'],
            'customer_locale' => ['nullable', Rule::in(array_keys(config('installbook.locales')))],
            'messaging_consent' => ['nullable', 'boolean'],

            // The machine. Serial is deliberately not required and never format-checked:
            // plates are dirty, half-hidden and printed in a dozen conventions, and a form
            // that refuses to save is a form the installer stops using.
            'appliance_type' => ['required', Rule::in(array_column(ApplianceType::cases(), 'value'))],
            'brand' => ['nullable', 'string', 'max:120'],
            'model' => ['nullable', 'string', 'max:120'],
            'serial' => ['nullable', 'string', 'max:120'],
            'extra_code' => ['nullable', 'string', 'max:120'],
            'capacity' => ['nullable', 'string', 'max:60'],
            'refrigerant' => ['nullable', 'string', 'max:40'],
            'plate_photo' => ['nullable', 'image', 'max:'.$maxKb],
            'unit_photo' => ['nullable', 'image', 'max:'.$maxKb],
            'plate_reading' => ['nullable', 'string', 'max:8000'],

            'installed_on' => ['nullable', 'date', 'before_or_equal:today'],
            'warranty_months' => ['nullable', 'integer', 'between:0,240'],
            'service_interval_months' => ['nullable', 'integer', 'between:0,120'],
            'service_required_for_warranty' => ['nullable', 'boolean'],
            'location_note' => ['nullable', 'string', 'max:160'],
            'notes' => ['nullable', 'string', 'max:2000'],
            'send_card' => ['nullable', 'boolean'],
        ];
    }

    protected function prepareForValidation(): void
    {
        $this->merge([
            'messaging_consent' => $this->boolean('messaging_consent'),
            'service_required_for_warranty' => $this->boolean('service_required_for_warranty'),
            'send_card' => $this->has('send_card') ? $this->boolean('send_card') : true,
            'serial' => is_string($this->serial) ? trim($this->serial) : $this->serial,
        ]);
    }

}
