@extends('layouts.app')
@section('title', __('ui.settings.title'))

@section('content')
<h1>{{ __('ui.settings.title') }}</h1>

<form method="post" action="{{ route('settings.update') }}" enctype="multipart/form-data">
    @csrf @method('PUT')

    <fieldset>
        <legend>{{ __('ui.settings.you') }}</legend>
        <label>{{ __('ui.settings.name') }}<input type="text" name="name" value="{{ old('name', $user->name) }}" required></label>
        <label>{{ __('ui.settings.business_name') }}<input type="text" name="business_name" value="{{ old('business_name', $user->business_name) }}"></label>
        <p class="field-hint">{{ __('ui.settings.business_hint') }}</p>
        <div class="grid2">
            <label>{{ __('ui.settings.phone') }}<input type="tel" name="phone" value="{{ old('phone', $user->phone) }}"></label>
            <label>{{ __('ui.settings.city') }}<input type="text" name="city" value="{{ old('city', $user->city) }}"></label>
        </div>
        <label>{{ __('ui.settings.registration_number') }}<input type="text" name="registration_number" value="{{ old('registration_number', $user->registration_number) }}"></label>
        <p class="field-hint">{{ __('ui.settings.registration_hint') }}</p>
        <label>{{ __('ui.settings.language') }}
            <select name="locale">
                @foreach (config('installbook.locales') as $code => $label)
                    <option value="{{ $code }}" @selected(old('locale', $user->locale) === $code)>{{ $label }}</option>
                @endforeach
            </select>
        </label>
        <label>{{ __('ui.settings.logo') }}<input type="file" name="logo" accept="image/*"></label>
        @if ($user->logo_path)<img class="thumb small" src="{{ \App\Support\Uploads::url($user->logo_path) }}" alt="">@endif
        <label>{{ __('ui.settings.card_footer') }}<textarea name="card_footer">{{ old('card_footer', $user->card_footer) }}</textarea></label>
    </fieldset>

    <fieldset>
        <legend>{{ __('ui.settings.reminders') }}</legend>
        <label>{{ __('ui.settings.lead_days') }}<input type="number" name="reminder_lead_days" min="7" max="84" value="{{ old('reminder_lead_days', $user->reminder_lead_days) }}" required></label>
        <label>{{ __('ui.settings.registration_window') }}<input type="number" name="registration_window_days" min="0" max="180" value="{{ old('registration_window_days', $user->registration_window_days) }}" required></label>
        <p class="field-hint">{{ __('ui.settings.registration_window_hint') }}</p>
    </fieldset>

    <button class="btn block">{{ __('ui.settings.save') }}</button>
</form>

<h2>{{ __('ui.settings.channels') }}</h2>
<div class="card">
    <p>{{ __('ui.settings.driver_'.$driver) }}</p>
    <p class="small muted">{{ $ocr === 'none' ? __('ui.settings.ocr_off') : __('ui.settings.ocr_on') }}</p>
    <p class="field-hint mono">INSTALLBOOK_MESSAGING_DRIVER={{ $driver }} · INSTALLBOOK_OCR_DRIVER={{ $ocr }}</p>
</div>

<form method="post" action="{{ route('logout') }}">
    @csrf
    <button class="btn ghost block">{{ __('ui.nav.logout') }}</button>
</form>
@endsection
