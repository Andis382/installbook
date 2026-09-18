@extends('layouts.app')
@section('title', __('ui.dashboard.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ now()->translatedFormat('l j F Y') }}</span>
    <h1>{{ __('ui.dashboard.title') }}</h1>
</div>

{{--
    Four numbers, in the order they cost money: what is already late, what is
    about to be, what loses its warranty if nobody registers it, and how much
    is on the books at all. Each one is the door into the list behind it.
--}}
<div class="counters">
    <a class="counter {{ $counts['overdue'] ? 'is-danger' : '' }}" href="{{ route('due') }}">
        <span class="micro">{{ __('ui.dashboard.overdue') }}</span>
        <span class="v">{{ $counts['overdue'] }}</span>
    </a>
    <a class="counter {{ $counts['due_30'] ? 'is-warn' : '' }}" href="{{ route('due') }}">
        <span class="micro">{{ __('ui.dashboard.due_30') }}</span>
        <span class="v">{{ $counts['due_30'] }}</span>
    </a>
    <a class="counter" href="{{ route('to-register') }}">
        <span class="micro">{{ __('ui.dashboard.to_register') }}</span>
        <span class="v">{{ $counts['to_register'] }}</span>
    </a>
    <a class="counter" href="{{ route('installations.index') }}">
        <span class="micro">{{ __('ui.dashboard.units') }}</span>
        <span class="v">{{ $counts['installs'] }}</span>
    </a>
</div>

@if ($counts['installs'] === 0)
    <div class="panel">
        <div class="empty">
            <x-icon name="book" size="44" />
            <p>{{ __('ui.dashboard.empty') }}</p>
        </div>
        <a class="btn big block" href="{{ route('installations.create') }}">
            <x-icon name="camera" size="20" />
            {{ __('ui.dashboard.first_install') }}
        </a>
    </div>
@else
    <a class="btn big block" href="{{ route('installations.create') }}">
        <x-icon name="plus" size="20" />
        {{ __('ui.nav.new_install') }}
    </a>
@endif

@if ($counts['outbox'] || $counts['bookings'])
    <div class="actions" style="margin-top:var(--s-3)">
        @if ($counts['bookings'])
            <a class="btn small" href="{{ route('bookings.index') }}">
                <x-icon name="bell" size="16" />
                {{ __('ui.dashboard.bookings') }} <span class="num">{{ $counts['bookings'] }}</span>
            </a>
        @endif
        @if ($counts['outbox'])
            <a class="btn ghost small" href="{{ route('outbox.index') }}">
                <x-icon name="outbox" size="16" />
                {{ __('ui.dashboard.outbox') }} <span class="num">{{ $counts['outbox'] }}</span>
            </a>
        @endif
    </div>
@endif

@if ($toRegister->isNotEmpty())
    <h2>{{ __('ui.dashboard.register_soon') }}</h2>
    <div class="panel flush">
        <div class="rows">
            @foreach ($toRegister as $install)
                @php($left = $install->registrationDaysLeft($today))
                <a class="entry" href="{{ route('installations.show', $install) }}">
                    <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
                    <span class="entry-body">
                        <span class="entry-title">{{ $install->unitName() }}</span>
                        <span class="entry-sub">
                            {{ $install->customer?->displayName() }}
                            · <span class="mono">{{ $install->serial ?: __('ui.show.no_serial') }}</span>
                        </span>
                    </span>
                    <span class="entry-side">
                        <x-tag :tone="$left < 0 ? 'danger' : ($left <= 7 ? 'warn' : 'quiet')"
                               :icon="$left < 0 ? 'warning' : 'clock'">
                            {{ $left < 0 ? __('ui.register.overdue') : __('ui.register.days_left', ['days' => $left]) }}
                        </x-tag>
                    </span>
                </a>
            @endforeach
        </div>
    </div>
@endif

@if ($dueSoon->isNotEmpty())
    <h2>{{ __('ui.dashboard.due_soon') }}</h2>
    <div class="panel flush">
        <div class="rows">
            @foreach ($dueSoon as $install)
                @php($days = $install->serviceDaysAway($today))
                <a class="entry" href="{{ route('installations.show', $install) }}">
                    <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
                    <span class="entry-body">
                        <span class="entry-title">{{ $install->unitName() }}</span>
                        <span class="entry-sub">{{ $install->customer?->displayName() }}
                            @if ($install->customer?->address) · {{ $install->customer->address }} @endif
                        </span>
                    </span>
                    <span class="entry-side">
                        <x-tag :tone="$days < 0 ? 'danger' : 'warn'" :icon="$days < 0 ? 'warning' : 'calendar'">
                            <span class="num">{{ $install->next_service_due_on->format('d/m/Y') }}</span>
                        </x-tag>
                    </span>
                </a>
            @endforeach
        </div>
    </div>
@endif

@if ($recent->isNotEmpty())
    <h2>{{ __('ui.dashboard.recent') }}</h2>
    <div class="panel flush">
        <div class="rows">
            @foreach ($recent as $install)
                <a class="entry" href="{{ route('installations.show', $install) }}">
                    <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
                    <span class="entry-body">
                        <span class="entry-title">{{ $install->unitName() }}</span>
                        <span class="entry-sub">{{ $install->customer?->displayName() }}</span>
                    </span>
                    <span class="entry-side small muted num">{{ $install->installed_on->format('d/m/Y') }}</span>
                </a>
            @endforeach
        </div>
    </div>
@endif
@endsection
