@extends('layouts.public')
@section('title', $install->unitName().' · '.$installer->displayName())

@section('content')
<div class="cardhead">
    @if ($installer->logo_path)
        <img class="logo" src="{{ \App\Support\Uploads::url($installer->logo_path) }}" alt="">
    @endif
    <div class="biz">{{ $installer->displayName() }}</div>
    <div class="sub">{{ __('card.title') }}</div>
</div>

<div class="card">
    <dl class="kv">
        <dt>{{ __('card.unit') }}</dt>
        <dd><strong>{{ $install->unitName() }}</strong><br><span class="small muted">{{ $install->typeLabel() }}</span></dd>
        <dt>{{ __('card.serial') }}</dt>
        <dd class="mono">{{ $install->serial ?: '—' }}</dd>
        <dt>{{ __('card.installed_on') }}</dt>
        <dd>{{ $install->installed_on->format('d/m/Y') }}</dd>
        @if ($install->location_note)
            <dt>{{ __('card.location') }}</dt><dd>{{ $install->location_note }}</dd>
        @endif
    </dl>

    @if ($install->plates->isNotEmpty())
        <p class="small muted">
            @foreach ($install->plates as $plate)
                {{ $plate->roleLabel() }}: <span class="mono">{{ $plate->serial ?: '—' }}</span>@if (! $loop->last) · @endif
            @endforeach
        </p>
    @endif
</div>

{{--
    Two guarantees, never merged. The statutory one is the law's, is owed by the seller and
    cannot be lost. The manufacturer's is a separate promise that may carry conditions. Showing
    them as one number is the small lie that lets a reminder pretend a missed service is a
    catastrophe, so they are kept apart here and in every message.
--}}
<div class="card">
    @if ($statutoryUntil)
        <div class="guarantee">
            <div class="label">{{ __('card.statutory') }}</div>
            <div class="value">{{ __('card.statutory_until', ['date' => $statutoryUntil->format('d/m/Y')]) }}</div>
            <p class="small muted">{{ __('card.statutory_explain') }}</p>
        </div>
    @endif

    <div class="guarantee manufacturer">
        <div class="label">{{ __('card.manufacturer') }}</div>
        <div class="value">
            {{ $install->warranty_expires_on ? __('card.manufacturer_until', ['date' => $install->warranty_expires_on->format('d/m/Y')]) : __('card.manufacturer_none') }}
        </div>
        @if ($install->service_required_for_warranty)
            <p class="small muted">{{ __('card.manufacturer_service_note') }}</p>
        @endif
    </div>
</div>

@if ($install->next_service_due_on && $install->status === 'active')
    <div class="card accent">
        <div class="label small muted">{{ __('card.next_service') }}</div>
        <div style="font-size:20px;font-weight:700">{{ $install->next_service_due_on->format('d/m/Y') }}</div>
        @if ($openBooking)
            <p class="small muted">{{ __('card.booking_open') }}</p>
        @else
            <a class="btn block" href="{{ route('card.book', $install->public_token) }}">{{ __('card.book') }}</a>
        @endif
    </div>
@endif

@if ($services->isNotEmpty())
    <h2>{{ __('card.service_history') }}</h2>
    <div class="card tight list">
        @foreach ($services as $visit)
            <div class="item">
                <span class="grow"><span class="title">{{ $visit->kindLabel() }}</span></span>
                <span class="right small muted">{{ $visit->performed_on->format('d/m/Y') }}</span>
            </div>
        @endforeach
    </div>
@endif

<div class="card">
    <h3>{{ __('card.contact') }}</h3>
    <p><strong>{{ $installer->displayName() }}</strong>
        @if ($installer->registration_number)<br><span class="small muted">{{ $installer->registration_number }}</span>@endif
    </p>
    @if ($installer->phone)
        <div class="row tight">
            <a class="btn block" href="tel:{{ $installer->phone }}">📞 {{ \App\Support\Phone::pretty($installer->phone) }}</a>
            <a class="btn wa block" href="https://wa.me/{{ \App\Support\Phone::digits($installer->phone) }}">💬 WhatsApp</a>
        </div>
    @endif
    @if ($installer->card_footer)<p class="small muted">{{ $installer->card_footer }}</p>@endif
</div>

<p class="footnote">{{ __('card.keep') }}</p>
<p class="footnote noprint">
    <a href="{{ route('card.print', $install->public_token) }}">{{ __('card.print') }}</a>
    @if ($customer && ! $customer->opted_out_at)
        · <a href="{{ route('card.stop', $install->public_token) }}">{{ __('card.stop') }}</a>
    @elseif ($customer)
        · <span class="muted">{{ __('card.opted_out_already') }}</span>
    @endif
</p>
@endsection
