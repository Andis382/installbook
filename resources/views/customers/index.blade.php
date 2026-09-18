@extends('layouts.app')
@section('title', __('ui.customers.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.customers.title') }}</h1>
</div>

<form method="get" class="searchbar" role="search">
    <label class="sr-only" for="f-q">{{ __('ui.customers.search') }}</label>
    <input class="grow" type="search" id="f-q" name="q" value="{{ $q }}" placeholder="{{ __('ui.customers.search') }}">
    <button class="btn">
        <x-icon name="search" size="18" />
        <span class="sr-only">{{ __('ui.common.search') }}</span>
    </button>
</form>

<div class="panel flush">
    <div class="rows">
        @forelse ($customers as $customer)
            <a class="entry" href="{{ route('customers.show', $customer) }}">
                <span class="glyph"><x-icon name="user" size="19" /></span>
                <span class="entry-body">
                    <span class="entry-title">{{ $customer->displayName() }}</span>
                    <span class="entry-sub">
                        <span class="mono">{{ \App\Support\Phone::pretty($customer->phone) }}</span>
                        @if ($customer->address) <br>{{ $customer->address }} @endif
                    </span>
                </span>
                <span class="entry-side">
                    <span class="small muted">{{ __('ui.customers.units', ['count' => $customer->installations_count]) }}</span>
                    @if ($customer->opted_out_at)
                        <x-tag icon="prohibit">{{ __('ui.customers.opted_out') }}</x-tag>
                    @elseif (! $customer->messaging_consent_at)
                        <x-tag tone="warn" icon="warning">{{ __('ui.customers.consent_no') }}</x-tag>
                    @endif
                </span>
            </a>
        @empty
            <div class="empty">
                <x-icon name="users" size="40" />
                <p>{{ __('ui.customers.none') }}</p>
            </div>
        @endforelse
    </div>
</div>

{{ $customers->withQueryString()->links() }}
@endsection
