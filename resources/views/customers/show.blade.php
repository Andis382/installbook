@extends('layouts.app')
@section('title', $customer->displayName())

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.show.customer') }}</span>
    <h1>{{ $customer->displayName() }}</h1>
</div>

<div class="panel">
    <dl class="spec">
        <div>
            <dt>{{ __('ui.install.phone') }}</dt>
            <dd><a class="mono" href="tel:{{ $customer->phone }}">{{ \App\Support\Phone::pretty($customer->phone) }}</a></dd>
        </div>
        @if ($customer->address)
            <div><dt>{{ __('ui.install.address') }}</dt><dd>{{ $customer->address }} {{ $customer->city }}</dd></div>
        @endif
        <div>
            <dt>{{ __('ui.settings.language') }}</dt>
            <dd>{{ config('installbook.locales')[$customer->locale] ?? $customer->locale }}</dd>
        </div>
        <div>
            <dt>{{ __('consent.ask') }}</dt>
            <dd>
                @if ($customer->opted_out_at)
                    <x-tag icon="prohibit">{{ __('ui.customers.opted_out') }}</x-tag>
                @elseif ($customer->messaging_consent_at)
                    <x-tag tone="ok" icon="check">{{ __('ui.customers.consent_yes') }}</x-tag>
                    <span class="note">
                        <span class="num">{{ $customer->messaging_consent_at->format('d/m/Y') }}</span> · {{ $customer->consent_text }}
                    </span>
                @else
                    <x-tag tone="warn" icon="warning">{{ __('ui.customers.consent_no') }}</x-tag>
                @endif
            </dd>
        </div>
    </dl>

    <div class="panel-foot">
        <form method="post" action="{{ route('customers.consent', $customer) }}">
            @csrf
            @if ($customer->canBeMessaged())
                <input type="hidden" name="granted" value="0">
                <button class="btn ghost small"><x-icon name="prohibit" size="15" />{{ __('ui.customers.withdraw') }}</button>
            @else
                <input type="hidden" name="granted" value="1">
                <button class="btn small"><x-icon name="check" size="15" />{{ __('ui.customers.grant') }}</button>
            @endif
        </form>
        <a class="btn ghost small" href="{{ route('customers.export', $customer) }}">
            <x-icon name="download" size="15" />{{ __('ui.customers.export') }}
        </a>
    </div>
</div>

<h2>{{ __('ui.nav.installs') }}</h2>
<div class="panel flush">
    <div class="rows">
        @forelse ($installs as $install)
            <a class="entry" href="{{ route('installations.show', $install) }}">
                <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
                <span class="entry-body">
                    <span class="entry-title">{{ $install->unitName() }}</span>
                    <span class="entry-sub mono">{{ $install->serial ?: __('ui.show.no_serial') }}</span>
                </span>
                <span class="entry-side small muted num">{{ $install->installed_on->format('d/m/Y') }}</span>
            </a>
        @empty
            <div class="empty"><x-icon name="book" size="36" /><p>{{ __('ui.common.none') }}</p></div>
        @endforelse
    </div>
</div>

{{--
    Erasure lives behind a disclosure, at the bottom, away from everything else.
    It is separated from the ordinary actions on purpose: this is the one button
    on the screen that destroys the installer's own proof of what he fitted.
--}}
<details class="more">
    <summary style="color:var(--danger)">
        <x-icon name="trash" size="18" />
        {{ __('ui.customers.delete') }}
    </summary>
    <div class="inner">
        <p class="small">{{ __('ui.customers.delete_warning') }}</p>
        <form method="post" action="{{ route('customers.destroy', $customer) }}">
            @csrf @method('DELETE')
            <label class="check">
                <input type="checkbox" name="confirm" value="1" required>
                <span class="what">{{ __('ui.customers.delete_confirm') }}</span>
            </label>
            <button class="btn danger block">{{ __('ui.customers.delete') }}</button>
        </form>
    </div>
</details>
@endsection
