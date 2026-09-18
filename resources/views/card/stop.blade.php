@extends('layouts.public')
@section('title', __('card.stop_title'))

@section('content')
<div class="sheet-head">
    <div class="biz">{{ $installer->displayName() }}</div>
    <span class="micro">{{ __('card.stop_title') }}</span>
</div>

@if ($customer?->opted_out_at)
    <div class="notice ok" role="status">
        <x-icon name="check-circle" size="20" />
        <div>{{ __('card.opted_out_already') }}</div>
    </div>
@else
    <div class="panel">
        <p>{{ __('card.stop_explain', ['installer' => $installer->displayName()]) }}</p>
        <form method="post" action="{{ route('card.stop.store', $install->public_token) }}">
            @csrf
            <button class="btn danger block big">{{ __('card.stop_confirm') }}</button>
        </form>
    </div>
@endif

<p class="center small">
    <a href="{{ route('card.show', $install->public_token) }}">{{ __('ui.common.back') }}</a>
</p>
@endsection
