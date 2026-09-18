@extends('layouts.app')
@section('title', __('ui.install.new'))

@section('content')
<h1>{{ __('ui.install.new') }}</h1>

{{--
    The whole product is this screen. It is used crouched behind a boiler, one-handed,
    at the end of the job. Order matters: photograph first (the record exists from that
    moment), then one tap for the type, then the phone number. Everything else is
    pre-filled, optional, or hidden until asked for.
--}}
<form method="post" action="{{ route('installations.store') }}" enctype="multipart/form-data" id="install-form">
    @csrf

    <div class="card accent">
        <h3>1. {{ __('ui.install.photo_plate') }}</h3>
        <input type="file" name="plate_photo" id="plate_photo" accept="image/*" capture="environment">
        <p class="field-hint">{{ __('ui.install.photo_hint') }}</p>
        <img id="plate_preview" class="thumb small hidden" alt="">
        <p id="ocr_state" class="field-hint hidden"></p>
        <input type="hidden" name="plate_reading" id="plate_reading">
    </div>

    <fieldset>
        <legend>2. {{ __('ui.install.what') }}</legend>
        <div class="types">
            @foreach ($types as $type)
                <input type="radio" name="appliance_type" value="{{ $type->value }}" id="t_{{ $type->value }}"
                       data-warranty="{{ $type->defaultWarrantyMonths() }}"
                       data-service="{{ $type->defaultServiceMonths() }}"
                       data-hint="{{ __('appliance.hint.'.$type->value) }}"
                       @checked(old('appliance_type') === $type->value)>
                <label for="t_{{ $type->value }}">
                    <span class="ico">{{ $type->icon() }}</span>
                    {{ $type->label() }}
                </label>
            @endforeach
        </div>
        <p class="field-hint" id="type_hint"></p>
    </fieldset>

    <fieldset>
        <legend>3. {{ __('ui.install.customer') }}</legend>
        <label>
            {{ __('ui.install.phone') }}
            <input type="tel" name="phone" inputmode="tel" autocomplete="tel" required value="{{ old('phone') }}" placeholder="069 123 4567">
        </label>
        <p class="field-hint">{{ __('ui.install.phone_hint') }}</p>
        <label>
            {{ __('ui.install.name') }} <span class="muted">({{ __('ui.common.optional') }})</span>
            <input type="text" name="customer_name" autocomplete="name" value="{{ old('customer_name') }}">
        </label>
        <label>
            {{ __('ui.install.address') }} <span class="muted">({{ __('ui.common.optional') }})</span>
            <input type="text" name="address" value="{{ old('address', $lastCustomer?->address) }}">
        </label>
        <div class="grid2">
            <label>
                {{ __('ui.install.city') }}
                <input type="text" name="city" value="{{ old('city', $lastCustomer?->city ?? auth()->user()->city) }}">
            </label>
            <label>
                &nbsp;
                <button type="button" class="btn ghost block" id="geo">📍 {{ __('ui.install.use_location') }}</button>
            </label>
        </div>
        <input type="hidden" name="lat" id="lat" value="{{ old('lat') }}">
        <input type="hidden" name="lng" id="lng" value="{{ old('lng') }}">
        <p class="field-hint hidden" id="geo_state">{{ __('ui.install.location_set') }}</p>

        <label class="inline">
            <input type="checkbox" name="messaging_consent" value="1" @checked(old('messaging_consent', true))>
            <span>
                {{ __('consent.short') }}
                <br><span class="field-hint">{{ $consentText }}</span>
            </span>
        </label>
    </fieldset>

    <fieldset>
        <legend>4. {{ __('ui.install.unit_details') }}</legend>
        <div class="grid2">
            <label>{{ __('appliance.field.brand') }}<input type="text" name="brand" id="brand" value="{{ old('brand') }}"></label>
            <label>{{ __('appliance.field.model') }}<input type="text" name="model" id="model" value="{{ old('model') }}"></label>
        </div>
        <label>
            {{ __('appliance.field.serial') }}
            <input type="text" name="serial" id="serial" class="mono" autocapitalize="characters" autocomplete="off" value="{{ old('serial') }}">
        </label>
        <p class="field-hint">{{ __('ui.install.serial_hint') }}</p>
        <label>
            {{ __('ui.install.where') }}
            <input type="text" name="location_note" value="{{ old('location_note') }}" placeholder="{{ __('ui.install.where_hint') }}">
        </label>
    </fieldset>

    <details class="card tight">
        <summary>{{ __('ui.install.advanced') }}</summary>
        <div class="grid2">
            <label>{{ __('ui.install.when') }}<input type="date" name="installed_on" value="{{ old('installed_on', now()->toDateString()) }}" max="{{ now()->toDateString() }}"></label>
            <label>{{ __('appliance.field.gc_number') }}<input type="text" name="extra_code" value="{{ old('extra_code') }}"></label>
        </div>
        <div class="grid2">
            <label>{{ __('ui.install.warranty_months') }}<input type="number" name="warranty_months" id="warranty_months" min="0" max="240" value="{{ old('warranty_months') }}"></label>
            <label>{{ __('ui.install.service_months') }}<input type="number" name="service_interval_months" id="service_months" min="0" max="120" value="{{ old('service_interval_months') }}"></label>
        </div>
        <p class="field-hint">{{ __('ui.install.defaults_note') }}</p>
        <div class="grid2">
            <label>{{ __('appliance.field.kw') }}<input type="text" name="capacity" id="capacity" value="{{ old('capacity') }}"></label>
            <label>{{ __('appliance.field.refrigerant') }}<input type="text" name="refrigerant" value="{{ old('refrigerant') }}"></label>
        </div>
        <label class="inline">
            <input type="checkbox" name="service_required_for_warranty" value="1" @checked(old('service_required_for_warranty'))>
            <span>
                {{ __('ui.install.service_required') }}
                <br><span class="field-hint">{{ __('ui.install.service_required_hint') }}</span>
            </span>
        </label>
        <label>{{ __('ui.common.photo') }} ({{ __('ui.show.unit') }})<input type="file" name="unit_photo" accept="image/*" capture="environment"></label>
        <label>{{ __('ui.install.notes') }}<textarea name="notes">{{ old('notes') }}</textarea></label>
    </details>

    <label class="inline">
        <input type="checkbox" name="send_card" value="1" @checked(old('send_card', true))>
        <span>{{ __('ui.install.send_card') }}</span>
    </label>

    <button type="submit" class="btn big block" id="submit">{{ __('ui.install.save') }}</button>
</form>
@endsection

@push('scripts')
<script>
(function () {
  const ocrEnabled = @json($ocrEnabled);
  const readUrl = @json(route('plate.read'));
  const token = @json(csrf_token());
  const strings = {
    reading: @json(__('ui.install.reading')),
    failed: @json(__('ui.install.read_failed')),
    ok: @json(__('ui.install.read_ok')),
    saving: @json(__('ui.install.saving')),
  };

  const $ = (id) => document.getElementById(id);
  const hint = $('type_hint');

  // Picking the type fills in the two intervals, so the installer never has to know them.
  document.querySelectorAll('.types input').forEach((radio) => {
    radio.addEventListener('change', () => {
      hint.textContent = radio.dataset.hint || '';
      const w = $('warranty_months'), s = $('service_months');
      if (w && !w.dataset.touched) w.value = radio.dataset.warranty;
      if (s && !s.dataset.touched) s.value = radio.dataset.service;
    });
  });
  ['warranty_months', 'service_months'].forEach((id) => {
    const el = $(id);
    if (el) el.addEventListener('input', () => { el.dataset.touched = '1'; });
  });
  const checked = document.querySelector('.types input:checked');
  if (checked) checked.dispatchEvent(new Event('change'));

  // The photo is shown immediately, and read only if plate reading is switched on.
  // A failed read is never in the way: the fields stay editable and empty.
  const photo = $('plate_photo');
  if (photo) photo.addEventListener('change', async () => {
    const file = photo.files && photo.files[0];
    if (!file) return;
    const preview = $('plate_preview');
    preview.src = URL.createObjectURL(file);
    preview.classList.remove('hidden');
    if (!ocrEnabled) return;

    const state = $('ocr_state');
    state.textContent = strings.reading;
    state.classList.remove('hidden');
    try {
      const body = new FormData();
      body.append('plate_photo', file);
      const type = document.querySelector('.types input:checked');
      if (type) body.append('appliance_type', type.value);
      const res = await fetch(readUrl, { method: 'POST', headers: { 'X-CSRF-TOKEN': token, 'Accept': 'application/json' }, body });
      const data = await res.json();
      const r = data.reading || {};
      let filled = 0;
      [['brand', 'brand'], ['model', 'model'], ['serial', 'serial'], ['capacity', 'capacity']].forEach(([key, id]) => {
        const el = $(id);
        if (el && !el.value && r[key]) { el.value = r[key]; filled++; }
      });
      $('plate_reading').value = JSON.stringify(r);
      state.textContent = filled ? strings.ok : strings.failed;
    } catch (e) {
      state.textContent = strings.failed;
    }
  });

  const geo = $('geo');
  if (geo) geo.addEventListener('click', () => {
    if (!navigator.geolocation) return;
    geo.disabled = true;
    navigator.geolocation.getCurrentPosition(
      (p) => {
        $('lat').value = p.coords.latitude.toFixed(6);
        $('lng').value = p.coords.longitude.toFixed(6);
        $('geo_state').classList.remove('hidden');
        geo.disabled = false;
      },
      () => { geo.disabled = false; },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  });

  const form = $('install-form');
  form.addEventListener('submit', () => {
    const btn = $('submit');
    btn.disabled = true;
    btn.textContent = strings.saving;
    // Re-enable if the browser restores the page from cache after a back navigation.
    setTimeout(() => { btn.disabled = false; btn.textContent = @json(__('ui.install.save')); }, 15000);
  });
})();
</script>
@endpush
