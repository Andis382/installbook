@extends('layouts.public')
@section('title', __('card.book_title'))

@section('content')
<div class="sheet-head">
    <div class="biz">{{ $installer->displayName() }}</div>
    <span class="micro">{{ __('card.book_title') }}</span>
</div>

<p>{{ __('card.book_intro', ['installer' => $installer->displayName()]) }}</p>
<p class="small muted">
    {{ $install->unitName() }}
    @if ($install->next_service_due_on)
        · {{ __('card.next_service') }}: <span class="num">{{ $install->next_service_due_on->format('d/m/Y') }}</span>
    @endif
</p>

<form method="post" action="{{ route('card.book.store', $install->public_token) }}" class="panel">
    @csrf
    <x-field name="preferred_window" control="select" :label="__('card.when_suits')"
             :options="['' => '—'] + collect($windows)->mapWithKeys(fn ($w) => [$w => __('card.window.'.$w)])->all()" />
    <x-field name="contact_phone" type="tel" :label="__('card.your_phone')" :value="$customer?->phone" inputmode="tel" autocomplete="tel" />
    <x-field name="note" control="textarea" :label="__('card.note')" maxlength="500" />
    <button class="btn block big">{{ __('card.send_request') }}</button>
</form>

<p class="center small">
    <a href="{{ route('card.show', $install->public_token) }}">{{ __('ui.common.back') }}</a>
</p>
@endsection
