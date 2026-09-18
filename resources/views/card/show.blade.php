@extends('layouts.public')
@section('title', $install->unitName().' · '.$installer->displayName())

@section('content')
<div class="sheet-head">
    @if ($installer->logo_path)
        <img class="logo" src="{{ \App\Support\Uploads::url($installer->logo_path) }}" alt="">
    @endif
    <div class="biz">{{ $installer->displayName() }}</div>
    <span class="micro">{{ __('card.title') }}</span>
</div>

<div class="panel">
    <span class="micro">{{ __('card.unit') }}</span>
    <div class="unit-name">{{ $install->unitName() }}</div>
    <p class="small muted" style="margin-top:2px">{{ $install->typeLabel() }}</p>

    <dl class="spec" style="margin-top:var(--s-3)">
        <div>
            <dt>{{ __('card.serial') }}</dt>
            <dd class="mono">{{ $install->serial ?: '—' }}</dd>
        </div>
        <div>
            <dt>{{ __('card.installed_on') }}</dt>
            <dd class="num">{{ $install->installed_on->format('d/m/Y') }}</dd>
        </div>
        @if ($install->location_note)
            <div><dt>{{ __('card.location') }}</dt><dd>{{ $install->location_note }}</dd></div>
        @endif
        @foreach ($install->plates as $plate)
            <div>
                <dt>{{ $plate->roleLabel() }}</dt>
                <dd class="mono">{{ $plate->serial ?: '—' }} {{ $plate->unitName() }}</dd>
            </div>
        @endforeach
    </dl>
</div>

{{--
    Two guarantees, never merged. The statutory one is the law's, is owed by the
    seller and cannot be lost. The manufacturer's is a separate promise that may
    carry conditions. Showing them as one number is the small lie that lets a
    reminder pretend a missed service is a catastrophe, so they are kept apart
    here and in every message — visually as well, with their own rule and their
    own icon, so nobody reads them as one fact.
--}}
<div class="panel">
    @if ($statutoryUntil)
        <div class="guarantee statutory">
            <span class="micro">{{ __('card.statutory') }}</span>
            <div class="value">
                <x-icon name="shield" size="20" />
                <span class="num">{{ __('card.statutory_until', ['date' => $statutoryUntil->format('d/m/Y')]) }}</span>
            </div>
            <p>{{ __('card.statutory_explain') }}</p>
        </div>
    @endif

    <div class="guarantee manufacturer">
        <span class="micro">{{ __('card.manufacturer') }}</span>
        <div class="value">
            <x-icon name="seal" size="20" />
            <span class="num">{{ $install->warranty_expires_on ? __('card.manufacturer_until', ['date' => $install->warranty_expires_on->format('d/m/Y')]) : __('card.manufacturer_none') }}</span>
        </div>
        @if ($install->service_required_for_warranty)
            <p>{{ __('card.manufacturer_service_note') }}</p>
        @endif
    </div>
</div>

@if ($install->next_service_due_on && $install->status === 'active')
    <div class="panel lead-primary">
        <span class="micro">{{ __('card.next_service') }}</span>
        <div class="due-big">
            <x-icon name="calendar" size="24" />
            {{ $install->next_service_due_on->format('d/m/Y') }}
        </div>
        @if ($openBooking)
            <p class="small muted">{{ __('card.booking_open') }}</p>
        @else
            <a class="btn block big" href="{{ route('card.book', $install->public_token) }}" style="margin-top:var(--s-3)">
                {{ __('card.book') }}
            </a>
        @endif
    </div>
@endif

@if ($services->isNotEmpty())
    <h2>{{ __('card.service_history') }}</h2>
    <div class="panel flush">
        <div class="rows">
            @foreach ($services as $visit)
                <div class="entry">
                    <span class="glyph"><x-icon name="toolbox" size="19" /></span>
                    <span class="entry-body"><span class="entry-title">{{ $visit->kindLabel() }}</span></span>
                    <span class="entry-side small muted num">{{ $visit->performed_on->format('d/m/Y') }}</span>
                </div>
            @endforeach
        </div>
    </div>
@endif

<div class="panel">
    <span class="micro">{{ __('card.contact') }}</span>
    <p><strong>{{ $installer->displayName() }}</strong></p>
    @if ($installer->registration_number)
        <p class="small muted mono">{{ $installer->registration_number }}</p>
    @endif
    @if ($installer->phone)
        <div class="stack" style="margin-top:var(--s-3)">
            <a class="btn block" href="tel:{{ $installer->phone }}">
                <x-icon name="phone" size="18" />
                <span class="mono">{{ \App\Support\Phone::pretty($installer->phone) }}</span>
            </a>
            <a class="btn whatsapp block" href="https://wa.me/{{ \App\Support\Phone::digits($installer->phone) }}">
                <x-icon name="whatsapp" size="18" />
                WhatsApp
            </a>
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
