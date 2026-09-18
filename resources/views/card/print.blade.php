@extends('layouts.public')
@section('title', __('card.title'))

@section('content')
<div class="sheet-head">
    @if ($installer->logo_path)<img class="logo" src="{{ \App\Support\Uploads::url($installer->logo_path) }}" alt="">@endif
    <div class="biz">{{ $installer->displayName() }}</div>
    <span class="micro">{{ __('card.title') }}</span>
</div>

<div class="panel">
    <dl class="spec">
        <div>
            <dt>{{ __('card.installed_by') }}</dt>
            <dd>
                {{ $installer->displayName() }}@if ($installer->phone), <span class="mono">{{ \App\Support\Phone::pretty($installer->phone) }}</span>@endif
                @if ($installer->registration_number)<span class="note mono">{{ $installer->registration_number }}</span>@endif
            </dd>
        </div>
        <div><dt>{{ __('card.unit') }}</dt><dd>{{ $install->unitName() }} <span class="note">{{ $install->typeLabel() }}</span></dd></div>
        <div><dt>{{ __('card.serial') }}</dt><dd class="mono">{{ $install->serial ?: '—' }}</dd></div>
        @foreach ($install->plates as $plate)
            <div><dt>{{ $plate->roleLabel() }}</dt><dd class="mono">{{ $plate->serial ?: '—' }} {{ $plate->unitName() }}</dd></div>
        @endforeach
        <div><dt>{{ __('card.installed_on') }}</dt><dd class="num">{{ $install->installed_on->format('d/m/Y') }}</dd></div>
        @if ($customer)
            <div>
                <dt>{{ __('ui.show.customer') }}</dt>
                <dd>{{ $customer->displayName() }}@if ($customer->address)<span class="note">{{ $customer->address }} {{ $customer->city }}</span>@endif</dd>
            </div>
        @endif
        @if ($statutoryUntil)
            <div><dt>{{ __('card.statutory') }}</dt><dd class="num">{{ $statutoryUntil->format('d/m/Y') }}</dd></div>
        @endif
        <div><dt>{{ __('card.manufacturer') }}</dt><dd class="num">{{ $install->warranty_expires_on?->format('d/m/Y') ?? '—' }}</dd></div>
        <div><dt>{{ __('card.next_service') }}</dt><dd class="num">{{ $install->next_service_due_on?->format('d/m/Y') ?? '—' }}</dd></div>
    </dl>
    <p class="small mono breakable" style="margin-top:var(--s-3)">{{ route('card.show', $install->public_token) }}</p>
</div>

@if ($services->isNotEmpty())
    <div class="panel">
        <span class="micro">{{ __('card.service_history') }}</span>
        @foreach ($services as $visit)
            <p class="small"><span class="num">{{ $visit->performed_on->format('d/m/Y') }}</span> — {{ $visit->kindLabel() }} {{ $visit->notes }}</p>
        @endforeach
    </div>
@endif

<p class="footnote">{{ __('card.keep') }}</p>
<p class="noprint center">
    <button class="btn" onclick="window.print()"><x-icon name="printer" size="18" />{{ __('card.print') }}</button>
</p>
@endsection
