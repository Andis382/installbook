@extends('layouts.app')
@section('title', __('ui.install.new'))

@section('content')
<div class="page-head">
    <span class="micro">{{ now()->translatedFormat('j F Y') }}</span>
    <h1>{{ __('ui.install.new') }}</h1>
</div>

{{--
    The whole product is this screen. It is used crouched behind a boiler, one
    handed, at the end of the job. Order matters: photograph first, because from
    that moment a record exists; then one tap for the type; then the number the
    card goes to. Everything else is pre-filled, optional, or folded away.

    Nothing here refuses to save. A record with a photo and no name beats the
    note that never got written because the form said no.
--}}
<form method="post" action="{{ route('installations.store') }}" enctype="multipart/form-data" id="install-form">
    @csrf

    <fieldset>
        <legend><span class="step">1</span>{{ __('ui.install.photo_plate') }}</legend>
        <div class="panel lead-primary">
            <input type="file" name="plate_photo" id="f-plate-photo" accept="image/*" capture="environment"
                   aria-describedby="plate-hint">
            <p class="hint" id="plate-hint">{{ __('ui.install.photo_hint') }}</p>
            <img id="plate_preview" class="shot small hidden" alt="">
            <p id="ocr_state" class="hint hidden" role="status"></p>
            <input type="hidden" name="plate_reading" id="plate_reading">
        </div>
    </fieldset>

    <fieldset>
        <legend><span class="step">2</span>{{ __('ui.install.what') }}</legend>
        <div class="types" role="radiogroup" aria-label="{{ __('ui.a11y.unit_type') }}">
            @foreach ($types as $type)
                <input type="radio" name="appliance_type" value="{{ $type->value }}" id="t_{{ $type->value }}"
                       data-warranty="{{ $type->defaultWarrantyMonths() }}"
                       data-service="{{ $type->defaultServiceMonths() }}"
                       data-hint="{{ __('appliance.hint.'.$type->value) }}"
                       @checked(old('appliance_type') === $type->value)>
                <label for="t_{{ $type->value }}">
                    <x-icon :name="$type->icon()" size="26" />
                    {{ $type->label() }}
                </label>
            @endforeach
        </div>
        @error('appliance_type')
            <p class="field-error"><x-icon name="warning-circle" size="15" /><span>{{ $message }}</span></p>
        @enderror
        <p class="hint" id="type_hint"></p>
    </fieldset>

    <fieldset>
        <legend><span class="step">3</span>{{ __('ui.install.customer') }}</legend>

        <x-field name="phone" type="tel" :label="__('ui.install.phone')" :hint="__('ui.install.phone_hint')"
                 inputmode="tel" autocomplete="tel" required placeholder="069 123 4567" />

        <x-field name="customer_name" :label="__('ui.install.name')" optional autocomplete="name" />
        <x-field name="address" :label="__('ui.install.address')" optional :value="$lastCustomer?->address" />

        <div class="cols2">
            <x-field name="city" :label="__('ui.install.city')"
                     :value="$lastCustomer?->city ?? auth()->user()->city" autocomplete="address-level2" />
            <div class="field">
                <span class="name">{{ __('ui.install.use_location') }}</span>
                <button type="button" class="btn ghost block" id="geo">
                    <x-icon name="crosshair" size="18" />
                    {{ __('ui.install.use_location') }}
                </button>
                <p class="hint hidden" id="geo_state" role="status">{{ __('ui.install.location_set') }}</p>
            </div>
        </div>
        <input type="hidden" name="lat" id="lat" value="{{ old('lat') }}">
        <input type="hidden" name="lng" id="lng" value="{{ old('lng') }}">

        <label class="check">
            <input type="checkbox" name="messaging_consent" value="1" @checked(old('messaging_consent', true))>
            <span class="what">
                {{ __('consent.short') }}
                <span class="hint">{{ $consentText }}</span>
            </span>
        </label>
    </fieldset>

    <fieldset>
        <legend><span class="step">4</span>{{ __('ui.install.unit_details') }}</legend>
        <div class="cols2">
            <x-field name="brand" :label="__('appliance.field.brand')" autocomplete="off" />
            <x-field name="model" :label="__('appliance.field.model')" autocomplete="off" />
        </div>
        <x-field name="serial" :label="__('appliance.field.serial')" :hint="__('ui.install.serial_hint')"
                 class="mono" autocapitalize="characters" autocomplete="off" spellcheck="false" />
        <x-field name="location_note" :label="__('ui.install.where')" :placeholder="__('ui.install.where_hint')" />
    </fieldset>

    <details class="more">
        <summary>{{ __('ui.install.advanced') }}</summary>
        <div class="inner">
            <div class="cols2">
                <x-field name="installed_on" type="date" :label="__('ui.install.when')"
                         :value="now()->toDateString()" :max="now()->toDateString()" />
                <x-field name="extra_code" :label="__('appliance.field.gc_number')" />
            </div>
            <div class="cols2">
                <x-field name="warranty_months" type="number" :label="__('ui.install.warranty_months')"
                         min="0" max="240" inputmode="numeric" />
                <x-field name="service_interval_months" type="number" :label="__('ui.install.service_months')"
                         min="0" max="120" inputmode="numeric" />
            </div>
            <p class="hint">{{ __('ui.install.defaults_note') }}</p>
            <div class="cols2">
                <x-field name="capacity" :label="__('appliance.field.kw')" />
                <x-field name="refrigerant" :label="__('appliance.field.refrigerant')" />
            </div>

            <label class="check">
                <input type="checkbox" name="service_required_for_warranty" value="1"
                       @checked(old('service_required_for_warranty'))>
                <span class="what">
                    {{ __('ui.install.service_required') }}
                    <span class="hint">{{ __('ui.install.service_required_hint') }}</span>
                </span>
            </label>

            <x-field name="unit_photo" type="file" :label="__('ui.common.photo').' ('.__('ui.show.unit').')'"
                     accept="image/*" capture="environment" />
            <x-field name="notes" control="textarea" :label="__('ui.install.notes')" />
        </div>
    </details>

    <label class="check">
        <input type="checkbox" name="send_card" value="1" @checked(old('send_card', true))>
        <span class="what">{{ __('ui.install.send_card') }}</span>
    </label>

    <button type="submit" class="btn big block" id="submit">
        <x-icon name="check" size="20" />
        {{ __('ui.install.save') }}
    </button>
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
    save: @json(__('ui.install.save')),
  };

  const $ = (id) => document.getElementById(id);
  const hint = $('type_hint');

  // Picking the type fills in the two intervals, so the installer never has to
  // know them — until he overrides one, after which we stop touching it.
  document.querySelectorAll('.types input').forEach((radio) => {
    radio.addEventListener('change', () => {
      hint.textContent = radio.dataset.hint || '';
      const w = $('f-warranty-months'), s = $('f-service-interval-months');
      if (w && !w.dataset.touched) w.value = radio.dataset.warranty;
      if (s && !s.dataset.touched) s.value = radio.dataset.service;
    });
  });
  ['f-warranty-months', 'f-service-interval-months'].forEach((id) => {
    const el = $(id);
    if (el) el.addEventListener('input', () => { el.dataset.touched = '1'; });
  });
  const checked = document.querySelector('.types input:checked');
  if (checked) checked.dispatchEvent(new Event('change'));

  // The photo appears immediately, and is read only if plate reading is on.
  // A failed read is never in the way: the fields stay editable and empty.
  const photo = $('f-plate-photo');
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
      [['brand', 'f-brand'], ['model', 'f-model'], ['serial', 'f-serial'], ['capacity', 'f-capacity']].forEach(([key, id]) => {
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
    setTimeout(() => { btn.disabled = false; btn.textContent = strings.save; }, 15000);
  });
})();
</script>
@endpush
