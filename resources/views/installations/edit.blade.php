@extends('layouts.app')
@section('title', __('ui.show.edit'))

@section('content')
<h1>{{ __('ui.show.edit') }}</h1>
<p class="muted">{{ $install->unitName() }} · {{ $install->customer?->displayName() }}</p>

<form method="post" action="{{ route('installations.update', $install) }}" enctype="multipart/form-data" class="card">
    @csrf @method('PUT')

    <label>{{ __('ui.install.what') }}
        <select name="appliance_type">
            @foreach ($types as $type)
                <option value="{{ $type->value }}" @selected(old('appliance_type', $install->appliance_type) === $type->value)>{{ $type->icon() }} {{ $type->label() }}</option>
            @endforeach
        </select>
    </label>

    <div class="grid2">
        <label>{{ __('appliance.field.brand') }}<input type="text" name="brand" value="{{ old('brand', $install->brand) }}"></label>
        <label>{{ __('appliance.field.model') }}<input type="text" name="model" value="{{ old('model', $install->model) }}"></label>
    </div>
    <label>{{ __('appliance.field.serial') }}<input type="text" name="serial" class="mono" value="{{ old('serial', $install->serial) }}"></label>
    <div class="grid2">
        <label>{{ __('appliance.field.gc_number') }}<input type="text" name="extra_code" value="{{ old('extra_code', $install->extra_code) }}"></label>
        <label>{{ __('appliance.field.kw') }}<input type="text" name="capacity" value="{{ old('capacity', $install->capacity) }}"></label>
    </div>
    <label>{{ __('appliance.field.refrigerant') }}<input type="text" name="refrigerant" value="{{ old('refrigerant', $install->refrigerant) }}"></label>

    <div class="grid2">
        <label>{{ __('ui.install.when') }}<input type="date" name="installed_on" value="{{ old('installed_on', $install->installed_on->toDateString()) }}" max="{{ now()->toDateString() }}" required></label>
        <label>{{ __('ui.install.where') }}<input type="text" name="location_note" value="{{ old('location_note', $install->location_note) }}"></label>
    </div>
    <div class="grid2">
        <label>{{ __('ui.install.warranty_months') }}<input type="number" name="warranty_months" min="0" max="240" value="{{ old('warranty_months', $install->warranty_months) }}" required></label>
        <label>{{ __('ui.install.service_months') }}<input type="number" name="service_interval_months" min="0" max="120" value="{{ old('service_interval_months', $install->service_interval_months) }}" required></label>
    </div>

    <label class="inline">
        <input type="checkbox" name="service_required_for_warranty" value="1" @checked(old('service_required_for_warranty', $install->service_required_for_warranty))>
        <span>{{ __('ui.install.service_required') }}<br><span class="field-hint">{{ __('ui.install.service_required_hint') }}</span></span>
    </label>

    <label>{{ __('ui.install.photo_plate') }}<input type="file" name="plate_photo" accept="image/*" capture="environment"></label>
    <label>{{ __('ui.install.notes') }}<textarea name="notes">{{ old('notes', $install->notes) }}</textarea></label>

    <button class="btn block">{{ __('ui.common.save') }}</button>
    <a class="btn ghost block" href="{{ route('installations.show', $install) }}" style="margin-top:8px">{{ __('ui.common.cancel') }}</a>
</form>
@endsection
