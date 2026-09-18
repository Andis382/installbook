@extends('layouts.app')
@section('title', $install->unitName())

@section('content')
@php($today = now()->startOfDay())
@php($days = $install->serviceDaysAway($today))

<div class="page-head">
    <span class="micro">{{ $install->typeLabel() }}</span>
    <div class="row between">
        <h1 class="grow">{{ $install->unitName() }}</h1>
        @if ($install->status === 'archived')
            <x-tag icon="archive">{{ __('ui.show.archived') }}</x-tag>
        @endif
    </div>
    <p>
        {{ $install->customer?->displayName() }}
        @if ($install->location_note) · {{ $install->location_note }} @endif
    </p>
</div>

@if ($install->needsManufacturerRegistration())
    @php($left = $install->registrationDaysLeft($today))
    <div class="panel {{ $left < 0 ? 'lead-danger' : 'lead-warn' }}">
        <div class="row between">
            <span class="micro">{{ __('ui.show.registration') }}</span>
            <x-tag :tone="$left < 0 ? 'danger' : 'warn'" :icon="$left < 0 ? 'warning' : 'clock'">
                {{ $left < 0 ? __('ui.register.overdue') : __('ui.register.days_left', ['days' => $left]) }}
            </x-tag>
        </div>
        <p>{{ __('ui.show.register_by', ['date' => $install->registration_deadline_on->format('d/m/Y')]) }}</p>
        <div class="actions">
            <button type="button" class="btn ghost small"
                    data-copy="{{ $install->brand }} {{ $install->model }} / {{ $install->serial }} / {{ $install->installed_on->format('d/m/Y') }}">
                <x-icon name="copy" size="15" />
                <span>{{ __('ui.register.copy_details') }}</span>
            </button>
            <form method="post" action="{{ route('installations.registered', $install) }}">
                @csrf
                <button class="btn small"><x-icon name="check" size="15" />{{ __('ui.show.mark_registered') }}</button>
            </form>
        </div>
    </div>
@elseif ($install->manufacturer_registered_at)
    <div class="notice ok" role="note">
        <x-icon name="seal" size="20" />
        <div>
            {{ __('ui.show.registered_on', ['date' => $install->manufacturer_registered_at->format('d/m/Y')]) }}
            <form method="post" action="{{ route('installations.registered', $install) }}" style="display:inline">
                @csrf <button class="linkbtn small">{{ __('ui.show.mark_unregistered') }}</button>
            </form>
        </div>
    </div>
@endif

<div class="panel">
    <dl class="spec">
        <div>
            <dt>{{ __('appliance.field.serial') }}</dt>
            <dd class="mono">{{ $install->serial ?: __('ui.show.no_serial') }}</dd>
        </div>
        @if ($install->extra_code)
            <div><dt>{{ __('appliance.field.gc_number') }}</dt><dd class="mono">{{ $install->extra_code }}</dd></div>
        @endif
        <div>
            <dt>{{ __('card.installed_on') }}</dt>
            <dd class="num">{{ $install->installed_on->format('d/m/Y') }}</dd>
        </div>

        {{-- The two guarantees, apart. See the comment on the customer card. --}}
        <div>
            <dt>{{ __('ui.show.statutory') }}</dt>
            <dd>
                <span class="num">{{ $install->statutoryGuaranteeUntil()?->format('d/m/Y') ?? '—' }}</span>
                <span class="note">{{ __('ui.show.statutory_note') }}</span>
            </dd>
        </div>
        <div>
            <dt>{{ __('ui.show.manufacturer') }}</dt>
            <dd>
                <span class="num">{{ $install->warranty_expires_on?->format('d/m/Y') ?? '—' }}</span>
                @if ($install->warranty_expires_on)
                    <x-tag :tone="$install->warrantyIsActive() ? 'ok' : 'quiet'"
                           :icon="$install->warrantyIsActive() ? 'check' : 'close'">
                        {{ $install->warrantyIsActive() ? __('ui.common.active') : __('ui.common.no') }}
                    </x-tag>
                @endif
            </dd>
        </div>
        <div>
            <dt>{{ __('ui.show.next_service') }}</dt>
            <dd>
                @if ($install->next_service_due_on)
                    <span class="num">{{ $install->next_service_due_on->format('d/m/Y') }}</span>
                    <x-tag :tone="$days < 0 ? 'danger' : ($days <= 30 ? 'warn' : 'quiet')"
                           :icon="$days < 0 ? 'warning' : 'clock'">
                        {{ $days < 0 ? __('ui.show.overdue_by', ['days' => abs($days)]) : __('ui.show.due_in', ['days' => $days]) }}
                    </x-tag>
                @else — @endif
            </dd>
        </div>
        @if ($install->customer?->address)
            <div>
                <dt>{{ __('ui.install.address') }}</dt>
                <dd>
                    {{ $install->customer->address }} {{ $install->customer->city }}
                    @if ($install->customer->lat)
                        <a class="note" target="_blank" rel="noopener"
                           href="https://www.openstreetmap.org/?mlat={{ $install->customer->lat }}&mlon={{ $install->customer->lng }}#map=18/{{ $install->customer->lat }}/{{ $install->customer->lng }}">
                            <x-icon name="pin" size="14" style="display:inline-block;vertical-align:-2px" />
                            <span class="num">{{ $install->customer->lat }}, {{ $install->customer->lng }}</span>
                        </a>
                    @endif
                </dd>
            </div>
        @endif
        @if ($install->notes)
            <div><dt>{{ __('ui.install.notes') }}</dt><dd>{{ $install->notes }}</dd></div>
        @endif
    </dl>

    @if ($install->plate_photo_path)
        <img class="shot" style="margin-top:var(--s-3)" src="{{ \App\Support\Uploads::url($install->plate_photo_path) }}"
             alt="{{ __('ui.install.photo_plate') }}">
    @endif

    <div class="panel-foot">
        <a class="btn ghost small" href="{{ route('installations.edit', $install) }}">
            <x-icon name="edit" size="15" />{{ __('ui.show.edit') }}
        </a>
        @if ($install->customer)
            <a class="btn ghost small" href="{{ route('customers.show', $install->customer) }}">
                <x-icon name="user" size="15" />{{ __('ui.show.customer') }}
            </a>
        @endif
    </div>
</div>

<div class="panel lead-primary">
    <span class="micro">{{ __('ui.show.card_link') }}</span>
    <p class="mono small breakable">{{ route('card.show', $install->public_token) }}</p>
    <div class="actions">
        <button type="button" class="btn ghost small" data-copy="{{ route('card.show', $install->public_token) }}">
            <x-icon name="copy" size="15" /><span>{{ __('ui.show.copy') }}</span>
        </button>
        <a class="btn ghost small" href="{{ route('card.show', $install->public_token) }}" target="_blank" rel="noopener">
            <x-icon name="external" size="15" />{{ __('ui.show.open_card') }}
        </a>
        <form method="post" action="{{ route('installations.card', $install) }}">
            @csrf
            <button class="btn small"><x-icon name="outbox" size="15" />{{ __('ui.show.resend_card') }}</button>
        </form>
    </div>
</div>

<h2>{{ __('ui.show.record_visit') }}</h2>
<form method="post" action="{{ route('visits.store', $install) }}" enctype="multipart/form-data" class="panel">
    @csrf
    <div class="cols2">
        <x-field name="kind" control="select" :label="__('ui.visit.kind')" selected="service"
                 :options="collect(\App\Models\ServiceVisit::KINDS)->mapWithKeys(fn ($k) => [$k => __('visit.'.$k)])->all()" />
        <x-field name="performed_on" type="date" :label="__('ui.visit.date')"
                 :value="now()->toDateString()" :max="now()->toDateString()" />
    </div>
    <div class="cols2">
        <x-field name="price" type="number" :label="__('ui.visit.price')" step="0.01" min="0" inputmode="decimal" />
        <x-field name="photo" type="file" :label="__('ui.visit.photo')" accept="image/*" capture="environment" />
    </div>
    <x-field name="notes" control="textarea" :label="__('ui.visit.notes')" />
    <button class="btn block"><x-icon name="check" size="18" />{{ __('ui.visit.save') }}</button>
</form>

<h2>{{ __('ui.show.history') }}</h2>
<div class="panel flush">
    <div class="rows">
        @forelse ($install->visits as $visit)
            <div class="entry">
                <span class="glyph"><x-icon name="toolbox" size="19" /></span>
                <span class="entry-body">
                    <span class="entry-title">{{ $visit->kindLabel() }}</span>
                    <span class="entry-sub">
                        <span class="num">{{ $visit->performed_on->format('d/m/Y') }}</span>
                        @if ($visit->notes) · {{ $visit->notes }} @endif
                    </span>
                </span>
                <span class="entry-side small muted num">{{ $visit->priceLabel() }}</span>
            </div>
        @empty
            <div class="empty">
                <x-icon name="toolbox" size="36" />
                <p>{{ __('card.no_services') }}</p>
            </div>
        @endforelse
    </div>
</div>

<h2>{{ __('ui.show.plates') }}</h2>
<div class="panel flush">
    <div class="rows">
        @foreach ($install->plates as $plate)
            <div class="entry">
                <span class="glyph"><x-icon name="file" size="19" /></span>
                <span class="entry-body">
                    <span class="entry-title">{{ $plate->roleLabel() }}</span>
                    <span class="entry-sub"><span class="mono">{{ $plate->serial ?: '—' }}</span> {{ $plate->unitName() }}</span>
                </span>
                <span class="entry-side">
                    <form method="post" action="{{ route('plates.destroy', $plate) }}">
                        @csrf @method('DELETE')
                        <button class="linkbtn danger">{{ __('ui.common.remove') }}</button>
                    </form>
                </span>
            </div>
        @endforeach
    </div>
    <details class="more" style="margin:0;border:0;border-top:1px solid var(--line)">
        <summary>{{ __('ui.show.add_plate') }}</summary>
        <div class="inner">
            <form method="post" action="{{ route('plates.store', $install) }}" enctype="multipart/form-data">
                @csrf
                <div class="cols2">
                    <x-field name="role" control="select" :label="__('ui.show.plates')"
                             :options="collect(\App\Models\InstallationPlate::ROLES)->mapWithKeys(fn ($r) => [$r => __('plate.role.'.$r)])->all()" />
                    <x-field name="serial" :label="__('appliance.field.serial')" class="mono" autocapitalize="characters" />
                </div>
                <div class="cols2">
                    <x-field name="brand" :label="__('appliance.field.brand')" />
                    <x-field name="model" :label="__('appliance.field.model')" />
                </div>
                <x-field name="photo" type="file" :label="__('ui.common.photo')" accept="image/*" capture="environment" />
                <button class="btn small"><x-icon name="plus" size="15" />{{ __('ui.common.add') }}</button>
            </form>
        </div>
    </details>
</div>

@if ($reminders->where('status', 'pending')->isNotEmpty())
    <h2>{{ __('ui.show.reminders') }}</h2>
    <div class="panel flush">
        <div class="rows">
            @foreach ($reminders->where('status', 'pending') as $reminder)
                <div class="entry">
                    <span class="glyph"><x-icon name="hourglass" size="19" /></span>
                    <span class="entry-body">
                        <span class="entry-title">{{ $reminder->kindLabel() }}</span>
                        <span class="entry-sub num">{{ $reminder->fire_on->format('d/m/Y') }}</span>
                    </span>
                </div>
            @endforeach
        </div>
    </div>
@endif

@if ($messages->isNotEmpty())
    <h2>{{ __('ui.show.messages') }}</h2>
    <div class="panel flush">
        <div class="rows">
            @foreach ($messages as $message)
                <div class="entry">
                    <span class="glyph"><x-icon name="chat" size="19" /></span>
                    <span class="entry-body">
                        <span class="entry-title">{{ $message->templateLabel() }}</span>
                        <span class="entry-sub num">{{ $message->created_at->format('d/m/Y H:i') }}</span>
                    </span>
                    <span class="entry-side">
                        <x-tag :tone="$message->status === 'sent' ? 'ok' : ($message->status === 'failed' ? 'danger' : 'quiet')"
                               :icon="$message->status === 'sent' ? 'check' : ($message->status === 'failed' ? 'warning' : 'clock')">
                            {{ $message->statusLabel() }}
                        </x-tag>
                    </span>
                </div>
            @endforeach
        </div>
        <div class="panel-foot">
            <a class="btn ghost small" href="{{ route('outbox.index') }}">
                <x-icon name="outbox" size="15" />{{ __('ui.nav.outbox') }}
            </a>
        </div>
    </div>
@endif

@if ($install->status !== 'archived')
    <form method="post" action="{{ route('installations.archive', $install) }}"
          onsubmit="return confirm(@js(__('ui.show.archive_confirm')))">
        @csrf
        <button class="btn ghost block"><x-icon name="archive" size="18" />{{ __('ui.show.archive') }}</button>
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
    // Confirm in the button itself, keeping the icon, so nothing moves.
    const label = btn.querySelector('span') || btn;
    const original = label.textContent;
    label.textContent = @json(__('ui.show.copied'));
    setTimeout(() => { label.textContent = original; }, 1500);
  });
});
</script>
@endpush
