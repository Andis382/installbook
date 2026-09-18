@extends('layouts.public')
@section('title', __('card.book_title'))

@section('content')
<div class="cardhead">
    <div class="biz">{{ $installer->displayName() }}</div>
    <div class="sub">{{ __('card.book_title') }}</div>
</div>

<p>{{ __('card.book_intro', ['installer' => $installer->displayName()]) }}</p>
<p class="small muted">{{ $install->unitName() }}
    @if ($install->next_service_due_on) · {{ __('card.next_service') }}: {{ $install->next_service_due_on->format('d/m/Y') }}@endif
</p>

<form method="post" action="{{ route('card.book.store', $install->public_token) }}" class="card">
    @csrf
    <label>{{ __('card.when_suits') }}
        <select name="preferred_window">
            <option value="">—</option>
            @foreach ($windows as $window)
                <option value="{{ $window }}">{{ __('card.window.'.$window) }}</option>
            @endforeach
        </select>
    </label>
    <label>{{ __('card.your_phone') }}
        <input type="tel" name="contact_phone" value="{{ $customer?->phone }}" inputmode="tel">
    </label>
    <label>{{ __('card.note') }}<textarea name="note" maxlength="500"></textarea></label>
    <button class="btn block big">{{ __('card.send_request') }}</button>
</form>

<p class="center"><a href="{{ route('card.show', $install->public_token) }}">{{ __('ui.common.back') }}</a></p>
@endsection
