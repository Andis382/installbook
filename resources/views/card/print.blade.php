@extends('layouts.public')
@section('title', __('card.title'))

@section('content')
<div class="cardhead">
    @if ($installer->logo_path)<img class="logo" src="{{ \App\Support\Uploads::url($installer->logo_path) }}" alt="">@endif
    <div class="biz">{{ $installer->displayName() }}</div>
    <div class="sub">{{ __('card.title') }}</div>
</div>

<div class="card">
    <dl class="kv">
        <dt>{{ __('card.installed_by') }}</dt>
        <dd>{{ $installer->displayName() }}@if ($installer->phone), {{ \App\Support\Phone::pretty($installer->phone) }}@endif
            @if ($installer->registration_number)<br>{{ $installer->registration_number }}@endif
        </dd>
        <dt>{{ __('card.unit') }}</dt><dd>{{ $install->unitName() }} ({{ $install->typeLabel() }})</dd>
        <dt>{{ __('card.serial') }}</dt><dd class="mono">{{ $install->serial ?: '—' }}</dd>
        @foreach ($install->plates as $plate)
            <dt>{{ $plate->roleLabel() }}</dt><dd class="mono">{{ $plate->serial ?: '—' }} {{ $plate->unitName() }}</dd>
        @endforeach
        <dt>{{ __('card.installed_on') }}</dt><dd>{{ $install->installed_on->format('d/m/Y') }}</dd>
        @if ($customer)
            <dt>{{ __('ui.show.customer') }}</dt>
            <dd>{{ $customer->displayName() }}@if ($customer->address)<br>{{ $customer->address }} {{ $customer->city }}@endif</dd>
        @endif
        @if ($statutoryUntil)<dt>{{ __('card.statutory') }}</dt><dd>{{ $statutoryUntil->format('d/m/Y') }}</dd>@endif
        <dt>{{ __('card.manufacturer') }}</dt><dd>{{ $install->warranty_expires_on?->format('d/m/Y') ?? '—' }}</dd>
        <dt>{{ __('card.next_service') }}</dt><dd>{{ $install->next_service_due_on?->format('d/m/Y') ?? '—' }}</dd>
    </dl>
    <p class="small">{{ route('card.show', $install->public_token) }}</p>
</div>

@if ($services->isNotEmpty())
    <div class="card">
        <h3>{{ __('card.service_history') }}</h3>
        @foreach ($services as $visit)
            <p class="small">{{ $visit->performed_on->format('d/m/Y') }} — {{ $visit->kindLabel() }} {{ $visit->notes }}</p>
        @endforeach
    </div>
@endif

<p class="footnote">{{ __('card.keep') }}</p>
<p class="noprint center"><button class="btn" onclick="window.print()">{{ __('card.print') }}</button></p>
@endsection
