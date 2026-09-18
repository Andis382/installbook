@extends('layouts.app')
@section('title', __('ui.bookings.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.bookings.title') }}</h1>
</div>

@forelse ($bookings as $booking)
    <div class="panel {{ $booking->status === 'new' ? 'lead-primary' : '' }}">
        <div class="panel-head">
            <span class="micro">{{ __('ui.bookings.from', ['name' => $booking->installation->customer?->displayName() ?? '—']) }}</span>
            <x-tag :tone="$booking->status === 'new' ? 'warn' : 'quiet'"
                   :icon="$booking->status === 'new' ? 'bell' : 'check'">
                {{ $booking->statusLabel() }}
            </x-tag>
        </div>

        <p><strong>{{ $booking->installation->unitName() }}</strong></p>
        <p class="small muted num">{{ $booking->created_at->format('d/m/Y H:i') }}</p>

        @if ($booking->windowLabel())
            <p class="row"><x-icon name="calendar" size="18" /> {{ $booking->windowLabel() }}</p>
        @endif
        @if ($booking->note)<div class="draft">{{ $booking->note }}</div>@endif

        <div class="actions">
            @if ($booking->contact_phone)
                <a class="btn ghost small" href="tel:{{ $booking->contact_phone }}">
                    <x-icon name="phone" size="15" />
                    <span class="mono">{{ \App\Support\Phone::pretty($booking->contact_phone) }}</span>
                </a>
            @endif
            <a class="btn ghost small" href="{{ route('installations.show', $booking->installation) }}">
                <x-icon name="book" size="15" />{{ __('ui.show.unit') }}
            </a>
        </div>

        <form method="post" action="{{ route('bookings.update', $booking) }}" class="panel-foot">
            @csrf
            <div class="grow">
                <label class="sr-only" for="status-{{ $booking->id }}">{{ __('ui.bookings.set_status') }}</label>
                <select id="status-{{ $booking->id }}" name="status">
                    @foreach (['new', 'scheduled', 'done', 'declined'] as $s)
                        <option value="{{ $s }}" @selected($booking->status === $s)>{{ __('booking.status.'.$s) }}</option>
                    @endforeach
                </select>
            </div>
            <div class="grow">
                <label class="sr-only" for="when-{{ $booking->id }}">{{ __('ui.bookings.scheduled_for') }}</label>
                <input id="when-{{ $booking->id }}" type="date" name="scheduled_for" value="{{ $booking->scheduled_for?->toDateString() }}">
            </div>
            <button class="btn small">{{ __('ui.bookings.set_status') }}</button>
        </form>
    </div>
@empty
    <div class="panel">
        <div class="empty">
            <x-icon name="bell" size="40" />
            <p>{{ __('ui.bookings.none') }}</p>
        </div>
    </div>
@endforelse

{{ $bookings->withQueryString()->links() }}
@endsection
