@extends('layouts.app')
@section('title', __('ui.nav.installs'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.nav.installs') }}</h1>
</div>

<form method="get" class="searchbar" role="search">
    <label class="sr-only" for="f-q">{{ __('ui.common.search') }}</label>
    <input class="grow" type="search" id="f-q" name="q" value="{{ $q }}"
           placeholder="{{ __('appliance.field.serial') }}, {{ __('ui.install.name') }}, {{ __('ui.install.address') }}">
    @if ($archived)<input type="hidden" name="status" value="archived">@endif
    <button class="btn">
        <x-icon name="search" size="18" />
        <span class="sr-only">{{ __('ui.common.search') }}</span>
    </button>
</form>

<div class="filters">
    <a class="filter" href="{{ route('installations.index', ['q' => $q]) }}"
       @if (! $archived) aria-current="true" @endif>{{ __('ui.common.active') }}</a>
    <a class="filter" href="{{ route('installations.index', ['q' => $q, 'status' => 'archived']) }}"
       @if ($archived) aria-current="true" @endif>{{ __('ui.common.archived') }}</a>
    <a class="filter" href="{{ route('customers.index') }}">
        <x-icon name="users" size="15" />
        {{ __('ui.nav.customers') }}
    </a>
</div>

<div class="panel flush">
    <div class="rows">
        @forelse ($installs as $install)
            <a class="entry" href="{{ route('installations.show', $install) }}">
                <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
                <span class="entry-body">
                    <span class="entry-title">{{ $install->unitName() }}</span>
                    <span class="entry-sub">
                        {{ $install->customer?->displayName() }}
                        @if ($install->serial) · <span class="mono">{{ $install->serial }}</span> @endif
                        @if ($install->customer?->address) <br>{{ $install->customer->address }} @endif
                    </span>
                </span>
                <span class="entry-side">
                    <span class="small muted num">{{ $install->installed_on->format('m/Y') }}</span>
                    @if ($install->next_service_due_on)
                        <x-tag :tone="$install->isOverdue() ? 'danger' : 'quiet'"
                               :icon="$install->isOverdue() ? 'warning' : 'calendar'">
                            <span class="num">{{ $install->next_service_due_on->format('d/m/y') }}</span>
                        </x-tag>
                    @endif
                </span>
            </a>
        @empty
            <div class="empty">
                <x-icon name="search" size="40" />
                <p>{{ __('ui.due.none') }}</p>
            </div>
        @endforelse
    </div>
</div>

{{ $installs->withQueryString()->links() }}
@endsection
