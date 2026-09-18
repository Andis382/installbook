@extends('layouts.app')
@section('title', __('ui.register.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.register.title') }}</h1>
    <p>{{ __('ui.register.explain') }}</p>
</div>

<div class="panel flush">
    <div class="rows">
        @forelse ($installs as $install)
            @php($left = $install->registrationDaysLeft($today))
            <div class="entry">
                <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
                <span class="entry-body">
                    <a class="entry-title" href="{{ route('installations.show', $install) }}">{{ $install->unitName() }}</a>
                    <span class="entry-sub">
                        <span class="mono">{{ $install->serial ?: __('ui.show.no_serial') }}</span>
                        <br>{{ $install->customer?->displayName() }} · <span class="num">{{ $install->installed_on->format('d/m/Y') }}</span>
                    </span>
                </span>
                <span class="entry-side">
                    <x-tag :tone="$left < 0 ? 'danger' : ($left <= 7 ? 'warn' : 'quiet')"
                           :icon="$left < 0 ? 'warning' : 'clock'">
                        {{ $left < 0 ? __('ui.register.overdue') : __('ui.register.days_left', ['days' => $left]) }}
                    </x-tag>
                    <form method="post" action="{{ route('installations.registered', $install) }}">
                        @csrf
                        <button class="btn small">
                            <x-icon name="check" size="15" />
                            {{ __('ui.show.mark_registered') }}
                        </button>
                    </form>
                </span>
            </div>
        @empty
            <div class="empty">
                <x-icon name="check-circle" size="40" />
                <p>{{ __('ui.register.none') }}</p>
            </div>
        @endforelse
    </div>
</div>
@endsection
