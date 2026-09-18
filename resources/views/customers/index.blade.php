@extends('layouts.app')
@section('title', __('ui.customers.title'))

@section('content')
<h1>{{ __('ui.customers.title') }}</h1>

<form method="get" class="row tight">
    <input type="search" name="q" value="{{ $q }}" placeholder="{{ __('ui.customers.search') }}" class="grow">
    <button class="btn">{{ __('ui.common.search') }}</button>
</form>

<div class="card tight list">
    @forelse ($customers as $customer)
        <a class="item" href="{{ route('customers.show', $customer) }}">
            <span class="grow">
                <span class="title">{{ $customer->displayName() }}</span>
                <span class="sub">{{ \App\Support\Phone::pretty($customer->phone) }}
                    @if ($customer->address) · {{ $customer->address }}@endif
                </span>
            </span>
            <span class="right small muted">
                {{ __('ui.customers.units', ['count' => $customer->installations_count]) }}
                @if ($customer->opted_out_at)
                    <br><span class="pill quiet">{{ __('ui.customers.opted_out') }}</span>
                @elseif (! $customer->messaging_consent_at)
                    <br><span class="pill warn">{{ __('ui.customers.consent_no') }}</span>
                @endif
            </span>
        </a>
    @empty
        <div class="empty"><span class="big">👤</span>{{ __('ui.customers.none') }}</div>
    @endforelse
</div>

{{ $customers->links() }}
@endsection
