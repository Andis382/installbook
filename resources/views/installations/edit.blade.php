@extends('layouts.app')
@section('title', __('ui.show.edit'))

@section('content')
<div class="page-head">
    <span class="micro">{{ $install->customer?->displayName() }}</span>
    <h1>{{ __('ui.show.edit') }}</h1>
    <p>{{ $install->unitName() }}</p>
</div>

<form method="post" action="{{ route('installations.update', $install) }}" enctype="multipart/form-data" class="panel">
    @csrf @method('PUT')

    <x-field name="appliance_type" control="select" :label="__('ui.install.what')" :selected="$install->appliance_type"
             :options="collect($types)->mapWithKeys(fn ($t) => [$t->value => $t->label()])->all()" />

    <div class="cols2">
        <x-field name="brand" :label="__('appliance.field.brand')" :value="$install->brand" />
        <x-field name="model" :label="__('appliance.field.model')" :value="$install->model" />
    </div>

    <x-field name="serial" :label="__('appliance.field.serial')" :value="$install->serial"
             class="mono" autocapitalize="characters" spellcheck="false" />

    <div class="cols2">
        <x-field name="extra_code" :label="__('appliance.field.gc_number')" :value="$install->extra_code" />
        <x-field name="capacity" :label="__('appliance.field.kw')" :value="$install->capacity" />
    </div>
    <x-field name="refrigerant" :label="__('appliance.field.refrigerant')" :value="$install->refrigerant" />

    <div class="cols2">
        <x-field name="installed_on" type="date" :label="__('ui.install.when')"
                 :value="$install->installed_on->toDateString()" :max="now()->toDateString()" required />
        <x-field name="location_note" :label="__('ui.install.where')" :value="$install->location_note" />
    </div>

    <div class="cols2">
        <x-field name="warranty_months" type="number" :label="__('ui.install.warranty_months')"
                 :value="$install->warranty_months" min="0" max="240" inputmode="numeric" required />
        <x-field name="service_interval_months" type="number" :label="__('ui.install.service_months')"
                 :value="$install->service_interval_months" min="0" max="120" inputmode="numeric" required />
    </div>

    <label class="check">
        <input type="checkbox" name="service_required_for_warranty" value="1"
               @checked(old('service_required_for_warranty', $install->service_required_for_warranty))>
        <span class="what">
            {{ __('ui.install.service_required') }}
            <span class="hint">{{ __('ui.install.service_required_hint') }}</span>
        </span>
    </label>

    <x-field name="plate_photo" type="file" :label="__('ui.install.photo_plate')" accept="image/*" capture="environment" />
    <x-field name="notes" control="textarea" :label="__('ui.install.notes')" :value="$install->notes" />

    <button class="btn block"><x-icon name="check" size="18" />{{ __('ui.common.save') }}</button>
    <a class="btn ghost block" href="{{ route('installations.show', $install) }}" style="margin-top:var(--s-2)">{{ __('ui.common.cancel') }}</a>
</form>
@endsection
