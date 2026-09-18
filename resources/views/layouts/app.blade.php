<!DOCTYPE html>
<html lang="{{ app()->getLocale() }}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#0d6efd">
<title>@yield('title', __('ui.app_name'))</title>
<link rel="stylesheet" href="{{ asset('css/app.css') }}">
<link rel="manifest" href="{{ asset('manifest.webmanifest') }}">
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><rect width='100' height='100' rx='20' fill='%230d6efd'/><text x='50' y='70' font-size='58' text-anchor='middle' fill='white' font-family='sans-serif' font-weight='bold'>iB</text></svg>">
</head>
<body>
<header class="topbar">
    <a class="brand" href="{{ route('dashboard') }}">install<span>book</span></a>
    <div class="who">
        {{ auth()->user()->displayName() }}
        <br><a href="{{ route('settings.edit') }}" class="small">{{ __('ui.nav.settings') }}</a>
    </div>
</header>

<main class="wrap @yield('wrapclass')">
    @include('partials.flash')
    @yield('content')
</main>

@php($pending = auth()->user()->messages()->where('status', 'queued')->count())
@php($newBookings = auth()->user()->bookingRequests()->where('status', 'new')->count())

<nav class="tabbar">
    <a href="{{ route('dashboard') }}" class="{{ request()->routeIs('dashboard') ? 'on' : '' }}">
        <span class="ico">🏠</span>{{ __('ui.nav.dashboard') }}
    </a>
    <a href="{{ route('due') }}" class="{{ request()->routeIs('due') ? 'on' : '' }}">
        <span class="ico">📅</span>{{ __('ui.nav.due') }}
    </a>
    <a href="{{ route('installations.create') }}" class="{{ request()->routeIs('installations.create') ? 'on' : '' }}">
        <span class="ico">➕</span>{{ __('ui.nav.new_install') }}
    </a>
    <a href="{{ route('outbox.index') }}" class="{{ request()->routeIs('outbox.*') ? 'on' : '' }}">
        <span class="ico">📤</span>{{ __('ui.nav.outbox') }}@if($pending)<span class="badge">{{ $pending }}</span>@endif
    </a>
    <a href="{{ route('installations.index') }}" class="{{ request()->routeIs('installations.index') || request()->routeIs('customers.*') ? 'on' : '' }}">
        <span class="ico">📖</span>{{ __('ui.nav.installs') }}
    </a>
</nav>

@stack('scripts')
</body>
</html>
