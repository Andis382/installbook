@extends('layouts.app')
@section('title', __('ui.bookings.title'))

@section('content')
<h1>{{ __('ui.bookings.title') }}</h1>

@forelse ($bookings as $booking)
    <div class="card {{ $booking->status === 'new' ? 'accent' : '' }}">
        <div class="row between">
            <span>
                <strong>{{ $booking->installation->unitName() }}</strong>
                <br><span class="small muted">
                    {{ __('ui.bookings.from', ['name' => $booking->installation->customer?->displayName() ?? '—']) }}
                    · {{ $booking->created_at->format('d/m H:i') }}
                </span>
            </span>
            <span class="pill {{ $booking->status === 'new' ? 'warn' : 'quiet' }}">{{ $booking->statusLabel() }}</span>
        </div>

        @if ($booking->windowLabel())<p>🗓 {{ $booking->windowLabel() }}</p>@endif
        @if ($booking->note)<p class="msgbody">{{ $booking->note }}</p>@endif

        <div class="row tight">
            @if ($booking->contact_phone)
                <a class="btn ghost small" href="tel:{{ $booking->contact_phone }}">📞 {{ \App\Support\Phone::pretty($booking->contact_phone) }}</a>
            @endif
            <a class="btn ghost small" href="{{ route('installations.show', $booking->installation) }}">{{ __('ui.show.unit') }}</a>
        </div>

        <form method="post" action="{{ route('bookings.update', $booking) }}" class="row tight" style="margin-top:10px">
            @csrf
            <select name="status" class="grow">
                @foreach (['new', 'scheduled', 'done', 'declined'] as $s)
                    <option value="{{ $s }}" @selected($booking->status === $s)>{{ __('booking.status.'.$s) }}</option>
                @endforeach
            </select>
            <input type="date" name="scheduled_for" value="{{ $booking->scheduled_for?->toDateString() }}">
            <button class="btn small">{{ __('ui.bookings.set_status') }}</button>
        </form>
    </div>
@empty
    <div class="empty"><span class="big">🔔</span>{{ __('ui.bookings.none') }}</div>
@endforelse

{{ $bookings->links() }}
@endsection
