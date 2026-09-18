@extends('layouts.app')
@section('title', __('ui.dashboard.title'))

@section('content')
<h1>{{ __('ui.dashboard.title') }}</h1>

<div class="stats">
    <a class="stat {{ $counts['overdue'] ? 'hot' : '' }}" href="{{ route('due') }}">
        <span class="k">{{ __('ui.dashboard.overdue') }}</span>
        <span class="v">{{ $counts['overdue'] }}</span>
    </a>
    <a class="stat {{ $counts['due_30'] ? 'due' : '' }}" href="{{ route('due') }}">
        <span class="k">{{ __('ui.dashboard.due_30') }}</span>
        <span class="v">{{ $counts['due_30'] }}</span>
    </a>
    <a class="stat" href="{{ route('to-register') }}">
        <span class="k">{{ __('ui.dashboard.to_register') }}</span>
        <span class="v">{{ $counts['to_register'] }}</span>
    </a>
    <a class="stat" href="{{ route('installations.index') }}">
        <span class="k">{{ __('ui.dashboard.units') }}</span>
        <span class="v">{{ $counts['installs'] }}</span>
    </a>
</div>

@if ($counts['outbox'] || $counts['bookings'])
<div class="row tight">
    @if ($counts['outbox'])
        <a class="btn ghost small" href="{{ route('outbox.index') }}">📤 {{ __('ui.dashboard.outbox') }}: {{ $counts['outbox'] }}</a>
    @endif
    @if ($counts['bookings'])
        <a class="btn small" href="{{ route('bookings.index') }}">🔔 {{ __('ui.dashboard.bookings') }}: {{ $counts['bookings'] }}</a>
    @endif
</div>
@endif

@if ($counts['installs'] === 0)
    <div class="card center">
        <div class="empty">
            <span class="big">📖</span>
            {{ __('ui.dashboard.empty') }}
        </div>
        <a class="btn big block" href="{{ route('installations.create') }}">{{ __('ui.dashboard.first_install') }}</a>
    </div>
@else
    <a class="btn big block" href="{{ route('installations.create') }}">➕ {{ __('ui.nav.new_install') }}</a>
@endif

@if ($toRegister->isNotEmpty())
    <h2>{{ __('ui.dashboard.register_soon') }}</h2>
    <div class="card tight list">
        @foreach ($toRegister as $install)
            @php($left = $install->registrationDaysLeft($today))
            <a class="item" href="{{ route('installations.show', $install) }}">
                <span class="grow">
                    <span class="title">{{ $install->unitName() }}</span>
                    <span class="sub">{{ $install->customer?->displayName() }} · {{ $install->serial ?: __('ui.show.no_serial') }}</span>
                </span>
                <span class="right">
                    <span class="pill {{ $left < 0 ? 'danger' : ($left <= 7 ? 'warn' : 'quiet') }}">
                        {{ $left < 0 ? __('ui.register.overdue') : __('ui.register.days_left', ['days' => $left]) }}
                    </span>
                </span>
            </a>
        @endforeach
    </div>
@endif

@if ($dueSoon->isNotEmpty())
    <h2>{{ __('ui.dashboard.due_soon') }}</h2>
    <div class="card tight list">
        @foreach ($dueSoon as $install)
            @php($days = $install->serviceDaysAway($today))
            <a class="item" href="{{ route('installations.show', $install) }}">
                <span class="grow">
                    <span class="title">{{ $install->unitName() }}</span>
                    <span class="sub">{{ $install->customer?->displayName() }} · {{ $install->customer?->address }}</span>
                </span>
                <span class="right">
                    <span class="pill {{ $days < 0 ? 'danger' : 'warn' }}">
                        {{ $install->next_service_due_on->format('d/m/Y') }}
                    </span>
                </span>
            </a>
        @endforeach
    </div>
@endif

@if ($recent->isNotEmpty())
    <h2>{{ __('ui.dashboard.recent') }}</h2>
    <div class="card tight list">
        @foreach ($recent as $install)
            <a class="item" href="{{ route('installations.show', $install) }}">
                <span class="grow">
                    <span class="title">{{ $install->type()->icon() }} {{ $install->unitName() }}</span>
                    <span class="sub">{{ $install->customer?->displayName() }} · {{ $install->installed_on->format('d/m/Y') }}</span>
                </span>
            </a>
        @endforeach
    </div>
@endif
@endsection
