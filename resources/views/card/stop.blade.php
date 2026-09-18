@extends('layouts.public')
@section('title', __('card.stop_title'))

@section('content')
<div class="cardhead">
    <div class="biz">{{ $installer->displayName() }}</div>
    <div class="sub">{{ __('card.stop_title') }}</div>
</div>

@if ($customer?->opted_out_at)
    <div class="card"><p>{{ __('card.opted_out_already') }}</p></div>
@else
    <div class="card">
        <p>{{ __('card.stop_explain', ['installer' => $installer->displayName()]) }}</p>
        <form method="post" action="{{ route('card.stop.store', $install->public_token) }}">
            @csrf
            <button class="btn danger block">{{ __('card.stop_confirm') }}</button>
        </form>
    </div>
@endif

<p class="center"><a href="{{ route('card.show', $install->public_token) }}">{{ __('ui.common.back') }}</a></p>
@endsection
