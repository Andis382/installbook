@extends('layouts.app')
@section('title', __('ui.nav.installs'))

@section('content')
<h1>{{ __('ui.nav.installs') }}</h1>

<form method="get" class="row tight">
    <input type="search" name="q" value="{{ $q }}" placeholder="{{ __('appliance.field.serial') }}, {{ __('ui.install.name') }}, {{ __('ui.install.address') }}" class="grow">
    @if ($archived)<input type="hidden" name="status" value="archived">@endif
    <button class="btn">{{ __('ui.common.search') }}</button>
</form>

<div class="chips">
    <a class="chip {{ $archived ? '' : 'on' }}" href="{{ route('installations.index', ['q' => $q]) }}">{{ __('ui.common.active') }}</a>
    <a class="chip {{ $archived ? 'on' : '' }}" href="{{ route('installations.index', ['q' => $q, 'status' => 'archived']) }}">{{ __('ui.common.archived') }}</a>
    <a class="chip" href="{{ route('customers.index') }}">{{ __('ui.nav.customers') }}</a>
</div>

<div class="card tight list">
    @forelse ($installs as $install)
        <a class="item" href="{{ route('installations.show', $install) }}">
            <span class="grow">
                <span class="title">{{ $install->type()->icon() }} {{ $install->unitName() }}</span>
                <span class="sub">{{ $install->customer?->displayName() }}
                    @if ($install->serial) · <span class="mono">{{ $install->serial }}</span>@endif
                    @if ($install->customer?->address) · {{ $install->customer->address }}@endif
                </span>
            </span>
            <span class="right small muted">
                {{ $install->installed_on->format('m/Y') }}
                @if ($install->next_service_due_on)
                    <br><span class="pill {{ $install->isOverdue() ? 'danger' : 'quiet' }}">{{ $install->next_service_due_on->format('d/m/y') }}</span>
                @endif
            </span>
        </a>
    @empty
        <div class="empty"><span class="big">🔍</span>{{ __('ui.due.none') }}</div>
    @endforelse
</div>

{{ $installs->links() }}
@endsection
