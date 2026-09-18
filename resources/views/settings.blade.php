@extends('layouts.app')
@section('title', __('ui.settings.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.settings.title') }}</h1>
</div>

<form method="post" action="{{ route('settings.update') }}" enctype="multipart/form-data">
    @csrf @method('PUT')

    <fieldset>
        <legend>{{ __('ui.settings.you') }}</legend>
        <div class="panel">
            <x-field name="name" :label="__('ui.settings.name')" :value="$user->name" required autocomplete="name" />
            <x-field name="business_name" :label="__('ui.settings.business_name')" :value="$user->business_name"
                     :hint="__('ui.settings.business_hint')" />
            <div class="cols2">
                <x-field name="phone" type="tel" :label="__('ui.settings.phone')" :value="$user->phone" inputmode="tel" />
                <x-field name="city" :label="__('ui.settings.city')" :value="$user->city" />
            </div>
            <x-field name="registration_number" :label="__('ui.settings.registration_number')"
                     :value="$user->registration_number" :hint="__('ui.settings.registration_hint')" />
            <x-field name="locale" control="select" :label="__('ui.settings.language')"
                     :options="config('installbook.locales')" :selected="$user->locale" />
            <x-field name="logo" type="file" :label="__('ui.settings.logo')" accept="image/*" />
            @if ($user->logo_path)
                <img class="shot small" src="{{ \App\Support\Uploads::url($user->logo_path) }}" alt="">
            @endif
            <x-field name="card_footer" control="textarea" :label="__('ui.settings.card_footer')" :value="$user->card_footer" />
        </div>
    </fieldset>

    <fieldset>
        <legend>{{ __('ui.settings.reminders') }}</legend>
        <div class="panel">
            <x-field name="reminder_lead_days" type="number" :label="__('ui.settings.lead_days')"
                     :value="$user->reminder_lead_days" min="7" max="84" inputmode="numeric" required />
            <x-field name="registration_window_days" type="number" :label="__('ui.settings.registration_window')"
                     :value="$user->registration_window_days" min="0" max="180" inputmode="numeric" required
                     :hint="__('ui.settings.registration_window_hint')" />
        </div>
    </fieldset>

    <button class="btn block big"><x-icon name="check" size="20" />{{ __('ui.settings.save') }}</button>
</form>

<h2>{{ __('ui.settings.channels') }}</h2>
<div class="panel">
    <p>{{ __('ui.settings.driver_'.$driver) }}</p>
    <p class="small muted">{{ $ocr === 'none' ? __('ui.settings.ocr_off') : __('ui.settings.ocr_on') }}</p>
    <dl class="spec" style="margin-top:var(--s-3)">
        <div><dt>Messaging</dt><dd class="mono">{{ $driver }}</dd></div>
        <div><dt>Plate reading</dt><dd class="mono">{{ $ocr }}</dd></div>
    </dl>
</div>

<form method="post" action="{{ route('logout') }}" style="margin-top:var(--s-6)">
    @csrf
    <button class="btn ghost block"><x-icon name="logout" size="18" />{{ __('ui.nav.logout') }}</button>
</form>
@endsection
