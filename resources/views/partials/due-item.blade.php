@php($days = $install->serviceDaysAway())
<a class="item" href="{{ route('installations.show', $install) }}">
    <span class="grow">
        <span class="title">{{ $install->type()->icon() }} {{ $install->unitName() }}</span>
        <span class="sub">
            {{ $install->customer?->displayName() }}
            @if ($install->customer?->address) · {{ $install->customer->address }}@endif
            @if ($install->bookingRequests->isNotEmpty()) · <strong>{{ __('ui.due.booked') }}</strong>@endif
        </span>
    </span>
    <span class="right">
        <span class="pill {{ $days < 0 ? 'danger' : ($days <= 30 ? 'warn' : 'quiet') }}">{{ $install->next_service_due_on?->format('d/m/y') }}</span>
        @if ($install->customer?->phone)
            <br><a class="small" href="tel:{{ $install->customer->phone }}">📞 {{ \App\Support\Phone::pretty($install->customer->phone) }}</a>
        @endif
    </span>
</a>
