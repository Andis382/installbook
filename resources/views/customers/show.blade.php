@extends('layouts.app')
@section('title', $customer->displayName())

@section('content')
<h1>{{ $customer->displayName() }}</h1>

<div class="card">
    <dl class="kv">
        <dt>{{ __('ui.install.phone') }}</dt>
        <dd><a href="tel:{{ $customer->phone }}">{{ \App\Support\Phone::pretty($customer->phone) }}</a></dd>
        @if ($customer->address)<dt>{{ __('ui.install.address') }}</dt><dd>{{ $customer->address }} {{ $customer->city }}</dd>@endif
        <dt>{{ __('ui.settings.language') }}</dt>
        <dd>{{ config('installbook.locales')[$customer->locale] ?? $customer->locale }}</dd>
        <dt>{{ __('consent.ask') }}</dt>
        <dd>
            @if ($customer->opted_out_at)
                <span class="pill quiet">{{ __('ui.customers.opted_out') }}</span>
            @elseif ($customer->messaging_consent_at)
                <span class="pill ok">{{ __('ui.customers.consent_yes') }}</span>
                <br><span class="field-hint">{{ $customer->messaging_consent_at->format('d/m/Y') }} · {{ $customer->consent_text }}</span>
            @else
                <span class="pill warn">{{ __('ui.customers.consent_no') }}</span>
            @endif
        </dd>
    </dl>

    <form method="post" action="{{ route('customers.consent', $customer) }}" class="row tight">
        @csrf
        @if ($customer->canBeMessaged())
            <input type="hidden" name="granted" value="0">
            <button class="btn ghost small">{{ __('ui.customers.withdraw') }}</button>
        @else
            <input type="hidden" name="granted" value="1">
            <button class="btn small">{{ __('ui.customers.grant') }}</button>
        @endif
        <a class="btn ghost small" href="{{ route('customers.export', $customer) }}">{{ __('ui.customers.export') }}</a>
    </form>
</div>

<h2>{{ __('ui.nav.installs') }}</h2>
<div class="card tight list">
    @forelse ($installs as $install)
        <a class="item" href="{{ route('installations.show', $install) }}">
            <span class="grow">
                <span class="title">{{ $install->type()->icon() }} {{ $install->unitName() }}</span>
                <span class="sub mono">{{ $install->serial ?: __('ui.show.no_serial') }}</span>
            </span>
            <span class="right small muted">{{ $install->installed_on->format('d/m/Y') }}</span>
        </a>
    @empty
        <p class="muted">{{ __('ui.common.none') }}</p>
    @endforelse
</div>

<details class="card">
    <summary style="color:var(--danger)">{{ __('ui.customers.delete') }}</summary>
    <p class="small">{{ __('ui.customers.delete_warning') }}</p>
    <form method="post" action="{{ route('customers.destroy', $customer) }}">
        @csrf @method('DELETE')
        <label class="inline">
            <input type="checkbox" name="confirm" value="1" required>
            <span>{{ __('ui.customers.delete_confirm') }}</span>
        </label>
        <button class="btn danger block">{{ __('ui.customers.delete') }}</button>
    </form>
</details>
@endsection
