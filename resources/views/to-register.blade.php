@extends('layouts.app')
@section('title', __('ui.register.title'))

@section('content')
<h1>{{ __('ui.register.title') }}</h1>
<p class="muted">{{ __('ui.register.explain') }}</p>

<div class="card tight list">
    @forelse ($installs as $install)
        @php($left = $install->registrationDaysLeft($today))
        <div class="item">
            <span class="grow">
                <a class="title" href="{{ route('installations.show', $install) }}">{{ $install->unitName() }}</a>
                <span class="sub mono">{{ $install->serial ?: __('ui.show.no_serial') }}</span>
                <span class="sub">{{ $install->customer?->displayName() }} · {{ $install->installed_on->format('d/m/Y') }}</span>
            </span>
            <span class="right">
                <span class="pill {{ $left < 0 ? 'danger' : ($left <= 7 ? 'warn' : 'quiet') }}">
                    {{ $left < 0 ? __('ui.register.overdue') : __('ui.register.days_left', ['days' => $left]) }}
                </span>
                <br>
                <form method="post" action="{{ route('installations.registered', $install) }}" style="margin-top:6px">
                    @csrf <button class="btn small">{{ __('ui.show.mark_registered') }}</button>
                </form>
            </span>
        </div>
    @empty
        <div class="empty"><span class="big">✅</span>{{ __('ui.register.none') }}</div>
    @endforelse
</div>
@endsection
