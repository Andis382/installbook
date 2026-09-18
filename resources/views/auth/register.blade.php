@extends('layouts.plain')
@section('title', __('auth.register_title'))
@section('pageclass', 'narrow')

@section('topnav')
    <a class="btn ghost small" href="{{ route('login') }}">{{ __('ui.nav.login') }}</a>
@endsection

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('auth.register_title') }}</h1>
    <p>{{ __('auth.register_intro') }}</p>
</div>

<form method="post" action="{{ route('register') }}" class="panel">
    @csrf

    <x-field name="name" :label="__('ui.settings.name')" :value="$name ?? ''" required autofocus autocomplete="name" />
    <x-field name="business_name" :label="__('ui.settings.business_name')" optional :hint="__('ui.settings.business_hint')" />

    <div class="cols2">
        <x-field name="phone" type="tel" :label="__('ui.settings.phone')" inputmode="tel" autocomplete="tel" />
        <x-field name="city" :label="__('ui.settings.city')" autocomplete="address-level2" />
    </div>

    <x-field name="locale" control="select" :label="__('ui.settings.language')"
             :options="config('installbook.locales')" :selected="config('installbook.defaults.locale')" />

    <x-field name="email" type="email" :label="__('auth.email')" :value="$email ?? ''" required autocomplete="email" inputmode="email" />
    <x-field name="password" type="password" :label="__('auth.password')" required autocomplete="new-password" />
    <x-field name="password_confirmation" type="password" :label="__('auth.confirm_password')" required autocomplete="new-password" />

    <button class="btn block big">{{ __('ui.nav.register') }}</button>
</form>

<p class="center small">{{ __('auth.have_account') }} <a href="{{ route('login') }}">{{ __('ui.nav.login') }}</a></p>
@endsection
