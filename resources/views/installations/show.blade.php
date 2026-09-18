@extends('layouts.app')
@section('title', $install->unitName())

@section('content')
@php($today = now()->startOfDay())
@php($days = $install->serviceDaysAway($today))

<div class="row between">
    <h1>{{ $install->type()->icon() }} {{ $install->unitName() }}</h1>
    @if ($install->status === 'archived')<span class="pill quiet">{{ __('ui.show.archived') }}</span>@endif
</div>
<p class="muted">{{ $install->typeLabel() }} · {{ $install->customer?->displayName() }}
    @if ($install->location_note) · {{ $install->location_note }} @endif
</p>

@if ($install->needsManufacturerRegistration())
    @php($left = $install->registrationDaysLeft($today))
    <div class="card warn">
        <h3>{{ __('ui.show.registration') }}</h3>
        <p>{{ __('ui.show.register_by', ['date' => $install->registration_deadline_on->format('d/m/Y')]) }}
            <span class="pill {{ $left < 0 ? 'danger' : 'warn' }}">{{ $left < 0 ? __('ui.register.overdue') : __('ui.register.days_left', ['days' => $left]) }}</span>
        </p>
        <div class="row tight">
            <button type="button" class="btn ghost small" data-copy="{{ $install->brand }} {{ $install->model }} / {{ $install->serial }} / {{ $install->installed_on->format('d/m/Y') }}">{{ __('ui.register.copy_details') }}</button>
            <form method="post" action="{{ route('installations.registered', $install) }}">
                @csrf
                <button class="btn small">{{ __('ui.show.mark_registered') }}</button>
            </form>
        </div>
    </div>
@elseif ($install->manufacturer_registered_at)
    <p class="small muted">✅ {{ __('ui.show.registered_on', ['date' => $install->manufacturer_registered_at->format('d/m/Y')]) }}
        <form method="post" action="{{ route('installations.registered', $install) }}" style="display:inline">
            @csrf <button class="linkbtn small">{{ __('ui.show.mark_unregistered') }}</button>
        </form>
    </p>
@endif

<div class="card">
    <dl class="kv">
        <dt>{{ __('appliance.field.serial') }}</dt>
        <dd class="mono">{{ $install->serial ?: __('ui.show.no_serial') }}</dd>
        @if ($install->extra_code)<dt>{{ __('appliance.field.gc_number') }}</dt><dd class="mono">{{ $install->extra_code }}</dd>@endif
        <dt>{{ __('card.installed_on') }}</dt>
        <dd>{{ $install->installed_on->format('d/m/Y') }}</dd>
        <dt>{{ __('ui.show.statutory') }}</dt>
        <dd>{{ $install->statutoryGuaranteeUntil()?->format('d/m/Y') ?? '—' }}<br><span class="field-hint">{{ __('ui.show.statutory_note') }}</span></dd>
        <dt>{{ __('ui.show.manufacturer') }}</dt>
        <dd>
            {{ $install->warranty_expires_on?->format('d/m/Y') ?? '—' }}
            @if ($install->warranty_expires_on)
                <span class="pill {{ $install->warrantyIsActive() ? 'ok' : 'quiet' }}">{{ $install->warrantyIsActive() ? __('ui.common.active') : __('ui.common.no') }}</span>
            @endif
        </dd>
        <dt>{{ __('ui.show.next_service') }}</dt>
        <dd>
            @if ($install->next_service_due_on)
                {{ $install->next_service_due_on->format('d/m/Y') }}
                <span class="pill {{ $days < 0 ? 'danger' : ($days <= 30 ? 'warn' : 'quiet') }}">
                    {{ $days < 0 ? __('ui.show.overdue_by', ['days' => abs($days)]) : __('ui.show.due_in', ['days' => $days]) }}
                </span>
            @else — @endif
        </dd>
        @if ($install->customer?->address)
            <dt>{{ __('ui.install.address') }}</dt>
            <dd>{{ $install->customer->address }} {{ $install->customer->city }}
                @if ($install->customer->lat)
                    <br><a target="_blank" rel="noopener" href="https://www.openstreetmap.org/?mlat={{ $install->customer->lat }}&mlon={{ $install->customer->lng }}#map=18/{{ $install->customer->lat }}/{{ $install->customer->lng }}">🗺 {{ $install->customer->lat }}, {{ $install->customer->lng }}</a>
                @endif
            </dd>
        @endif
        @if ($install->notes)<dt>{{ __('ui.install.notes') }}</dt><dd>{{ $install->notes }}</dd>@endif
    </dl>

    @if ($install->plate_photo_path)
        <img class="thumb" src="{{ \App\Support\Uploads::url($install->plate_photo_path) }}" alt="{{ __('ui.install.photo_plate') }}">
    @endif

    <div class="row tight" style="margin-top:12px">
        <a class="btn ghost small" href="{{ route('installations.edit', $install) }}">{{ __('ui.show.edit') }}</a>
        <a class="btn ghost small" href="{{ route('customers.show', $install->customer) }}">{{ __('ui.show.customer') }}</a>
    </div>
</div>

<div class="card accent">
    <h3>{{ __('ui.show.card_link') }}</h3>
    <p class="mono small" style="word-break:break-all">{{ route('card.show', $install->public_token) }}</p>
    <div class="row tight">
        <button type="button" class="btn ghost small" data-copy="{{ route('card.show', $install->public_token) }}">{{ __('ui.show.copy') }}</button>
        <a class="btn ghost small" href="{{ route('card.show', $install->public_token) }}" target="_blank" rel="noopener">{{ __('ui.show.open_card') }}</a>
        <form method="post" action="{{ route('installations.card', $install) }}">
            @csrf <button class="btn small">{{ __('ui.show.resend_card') }}</button>
        </form>
    </div>
</div>

<h2>{{ __('ui.show.record_visit') }}</h2>
<form method="post" action="{{ route('visits.store', $install) }}" enctype="multipart/form-data" class="card">
    @csrf
    <div class="grid2">
        <label>{{ __('ui.visit.kind') }}
            <select name="kind">
                @foreach (\App\Models\ServiceVisit::KINDS as $kind)
                    <option value="{{ $kind }}" @selected($kind === 'service')>{{ __('visit.'.$kind) }}</option>
                @endforeach
            </select>
        </label>
        <label>{{ __('ui.visit.date') }}<input type="date" name="performed_on" value="{{ now()->toDateString() }}" max="{{ now()->toDateString() }}"></label>
    </div>
    <div class="grid2">
        <label>{{ __('ui.visit.price') }}<input type="number" name="price" step="0.01" min="0"></label>
        <label>{{ __('ui.visit.photo') }}<input type="file" name="photo" accept="image/*" capture="environment"></label>
    </div>
    <label>{{ __('ui.visit.notes') }}<textarea name="notes"></textarea></label>
    <button class="btn block">{{ __('ui.visit.save') }}</button>
</form>

<h2>{{ __('ui.show.history') }}</h2>
<div class="card tight list">
    @forelse ($install->visits as $visit)
        <div class="item">
            <span class="grow">
                <span class="title">{{ $visit->kindLabel() }}</span>
                <span class="sub">{{ $visit->performed_on->format('d/m/Y') }}@if ($visit->notes) · {{ $visit->notes }} @endif</span>
            </span>
            <span class="right small muted">{{ $visit->priceLabel() }}</span>
        </div>
    @empty
        <p class="muted">{{ __('card.no_services') }}</p>
    @endforelse
</div>

<h2>{{ __('ui.show.plates') }}</h2>
<div class="card tight">
    <div class="list">
        @foreach ($install->plates as $plate)
            <div class="item">
                <span class="grow">
                    <span class="title">{{ $plate->roleLabel() }}</span>
                    <span class="sub mono">{{ $plate->serial ?: '—' }} {{ $plate->unitName() }}</span>
                </span>
                <span class="right">
                    <form method="post" action="{{ route('plates.destroy', $plate) }}">
                        @csrf @method('DELETE')
                        <button class="linkbtn danger small">{{ __('ui.common.remove') }}</button>
                    </form>
                </span>
            </div>
        @endforeach
    </div>
    <details>
        <summary>{{ __('ui.show.add_plate') }}</summary>
        <form method="post" action="{{ route('plates.store', $install) }}" enctype="multipart/form-data">
            @csrf
            <div class="grid2">
                <label>{{ __('ui.show.plates') }}
                    <select name="role">
                        @foreach (\App\Models\InstallationPlate::ROLES as $role)
                            <option value="{{ $role }}">{{ __('plate.role.'.$role) }}</option>
                        @endforeach
                    </select>
                </label>
                <label>{{ __('appliance.field.serial') }}<input type="text" name="serial" class="mono"></label>
            </div>
            <div class="grid2">
                <label>{{ __('appliance.field.brand') }}<input type="text" name="brand"></label>
                <label>{{ __('appliance.field.model') }}<input type="text" name="model"></label>
            </div>
            <label>{{ __('ui.common.photo') }}<input type="file" name="photo" accept="image/*" capture="environment"></label>
            <button class="btn small">{{ __('ui.common.add') }}</button>
        </form>
    </details>
</div>

@if ($reminders->where('status', 'pending')->isNotEmpty())
    <h2>{{ __('ui.show.reminders') }}</h2>
    <div class="card tight list">
        @foreach ($reminders->where('status', 'pending') as $reminder)
            <div class="item">
                <span class="grow">
                    <span class="title">{{ $reminder->kindLabel() }}</span>
                    <span class="sub">{{ $reminder->fire_on->format('d/m/Y') }}</span>
                </span>
            </div>
        @endforeach
    </div>
@endif

@if ($messages->isNotEmpty())
    <h2>{{ __('ui.show.messages') }}</h2>
    <div class="card tight list">
        @foreach ($messages as $message)
            <div class="item">
                <span class="grow">
                    <span class="title">{{ $message->templateLabel() }}</span>
                    <span class="sub">{{ $message->created_at->format('d/m/Y H:i') }}</span>
                </span>
                <span class="right"><span class="pill {{ $message->status === 'sent' ? 'ok' : ($message->status === 'failed' ? 'danger' : 'quiet') }}">{{ $message->statusLabel() }}</span></span>
            </div>
        @endforeach
        <a class="btn ghost small" href="{{ route('outbox.index') }}">{{ __('ui.nav.outbox') }}</a>
    </div>
@endif

@if ($install->status !== 'archived')
    <form method="post" action="{{ route('installations.archive', $install) }}" onsubmit="return confirm(@js(__('ui.show.archive_confirm')))">
        @csrf
        <button class="btn ghost block">{{ __('ui.show.archive') }}</button>
    </form>
@endif
@endsection

@push('scripts')
<script>
document.querySelectorAll('[data-copy]').forEach((btn) => {
  btn.addEventListener('click', async () => {
    const text = btn.dataset.copy;
    try { await navigator.clipboard.writeText(text); }
    catch (e) {
      const ta = document.createElement('textarea');
      ta.value = text; document.body.appendChild(ta); ta.select();
      document.execCommand('copy'); ta.remove();
    }
    const original = btn.textContent;
    btn.textContent = @json(__('ui.show.copied'));
    setTimeout(() => { btn.textContent = original; }, 1500);
  });
});
</script>
@endpush
