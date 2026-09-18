<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
<head>
@include('partials.head')
<title>@yield('title', __('ui.app_name')) · {{ __('ui.app_name') }}</title>
<link rel="manifest" href="{{ asset('manifest.webmanifest') }}">
</head>
<body>
<a class="skip" href="#main">{{ __('ui.a11y.skip') }}</a>

<header class="topbar">
    <a class="brand" href="{{ route('dashboard') }}" aria-label="{{ __('ui.app_name') }}"><b>install</b><span>book</span></a>
    <div class="topbar-right">
        <span class="who">
            <strong>{{ auth()->user()->displayName() }}</strong>
            {{ auth()->user()->city }}
        </span>
        <a class="iconbtn" href="{{ route('settings.edit') }}" aria-label="{{ __('ui.nav.settings') }}"
           @if (request()->routeIs('settings.*')) aria-current="page" @endif>
            <x-icon name="settings" size="22" />
        </a>
    </div>
</header>

<main class="page @yield('pageclass')" id="main" tabindex="-1">
    @include('partials.flash')
    @yield('content')
</main>

@php($pending = auth()->user()->messages()->where('status', 'queued')->count())

{{--
    Five destinations, which is the limit before a bottom bar stops being
    navigation and starts being a menu. Each one carries an icon and the word
    for it: an icon on its own teaches nobody what is behind it.
--}}
<nav class="tabbar" aria-label="{{ __('ui.a11y.main_nav') }}">
    <a href="{{ route('dashboard') }}" @if (request()->routeIs('dashboard')) aria-current="page" @endif>
        <x-icon name="home" size="22" />
        <span class="label">{{ __('ui.nav.dashboard') }}</span>
    </a>
    <a href="{{ route('due') }}" @if (request()->routeIs('due')) aria-current="page" @endif>
        <x-icon name="calendar" size="22" />
        <span class="label">{{ __('ui.nav.due') }}</span>
    </a>
    <a href="{{ route('installations.create') }}" @if (request()->routeIs('installations.create')) aria-current="page" @endif>
        <x-icon name="plus" size="22" />
        <span class="label">{{ __('ui.nav.new_install') }}</span>
    </a>
    <a href="{{ route('outbox.index') }}" @if (request()->routeIs('outbox.*')) aria-current="page" @endif>
        <x-icon name="outbox" size="22" />
        <span class="label">{{ __('ui.nav.outbox') }}</span>
        @if ($pending)
            {{-- Announced as a sentence. A screen reader saying "nine" on its own means nothing. --}}
            <span class="count" aria-hidden="true">{{ $pending }}</span>
            <span class="sr-only">, {{ trans_choice('ui.a11y.outbox_waiting', $pending, ['count' => $pending]) }}</span>
        @endif
    </a>
    <a href="{{ route('installations.index') }}"
       @if (request()->routeIs('installations.index') || request()->routeIs('customers.*')) aria-current="page" @endif>
        <x-icon name="book" size="22" />
        <span class="label">{{ __('ui.nav.installs') }}</span>
    </a>
</nav>

@stack('scripts')
</body>
</html>
