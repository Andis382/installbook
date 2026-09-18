@php($days = $install->serviceDaysAway())
<a class="entry" href="{{ route('installations.show', $install) }}">
    <span class="glyph"><x-icon :name="$install->type()->icon()" size="19" /></span>
    <span class="entry-body">
        <span class="entry-title">{{ $install->unitName() }}</span>
        <span class="entry-sub">
            {{ $install->customer?->displayName() }}
            @if ($install->customer?->address) · {{ $install->customer->address }} @endif
            @if ($install->customer?->phone)
                <br><span class="mono">{{ \App\Support\Phone::pretty($install->customer->phone) }}</span>
            @endif
        </span>
    </span>
    <span class="entry-side">
        <x-tag :tone="$days < 0 ? 'danger' : ($days <= 30 ? 'warn' : 'quiet')"
               :icon="$days < 0 ? 'warning' : 'calendar'">
            <span class="num">{{ $install->next_service_due_on?->format('d/m/Y') }}</span>
        </x-tag>
        @if ($install->bookingRequests->isNotEmpty())
            <x-tag tone="info" icon="check">{{ __('ui.due.booked') }}</x-tag>
        @endif
    </span>
</a>
