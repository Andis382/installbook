@extends('layouts.app')
@section('title', __('ui.outbox.title'))

@section('content')
<div class="page-head">
    <span class="micro">{{ __('ui.app_name') }}</span>
    <h1>{{ __('ui.outbox.title') }}</h1>
    <p>
        @if ($requiresTap)
            {{ __('ui.outbox.manual_explain') }}
        @else
            {{ __('ui.outbox.auto_explain', ['driver' => $driver]) }}
        @endif
    </p>
</div>

<div class="filters">
    @foreach (['queued', 'sent', 'failed', 'skipped'] as $key)
        <a class="filter" href="{{ route('outbox.index', ['status' => $key]) }}"
           @if ($status === $key) aria-current="true" @endif>
            {{ __('message.status.'.$key) }} <span class="n">{{ $counts[$key] }}</span>
        </a>
    @endforeach
</div>

@forelse ($messages as $message)
    <div class="panel {{ $message->status === 'queued' ? 'lead-primary' : '' }}">
        <div class="panel-head">
            <span class="micro">
                {{ $message->templateLabel() }}
                · {{ strtoupper($message->locale) }}
            </span>
            <x-tag :tone="match ($message->status) { 'sent' => 'ok', 'failed' => 'danger', 'skipped' => 'quiet', default => 'warn' }"
                   :icon="match ($message->status) { 'sent' => 'check', 'failed' => 'warning', 'skipped' => 'prohibit', default => 'clock' }">
                {{ $message->statusLabel() }}
            </x-tag>
        </div>

        <p class="small muted">
            {{ $message->customer?->displayName() ?? \App\Support\Phone::pretty($message->to_phone) }}
            · <span class="num">{{ $message->created_at->format('d/m H:i') }}</span>
        </p>

        {{-- Exactly what the customer will receive, before anybody sends it. --}}
        <div class="draft">{{ $message->body }}</div>

        @if ($message->status === 'skipped')
            <p class="small muted">{{ __('ui.outbox.skipped_reason', ['reason' => __('ui.outbox.reason.'.$message->error, [], null) ?: $message->error]) }}</p>
        @elseif ($message->error)
            <p class="field-error"><x-icon name="warning-circle" size="15" /><span>{{ $message->error }}</span></p>
        @endif

        @if ($message->status === 'queued')
            <a class="btn whatsapp block" href="{{ $message->whatsappLink() }}" target="_blank" rel="noopener">
                <x-icon name="whatsapp" size="20" />
                {{ __('ui.outbox.send') }}
            </a>
            <div class="actions" style="margin-top:var(--s-2)">
                <button type="button" class="btn ghost small" data-copy="{{ $message->body }}">
                    <x-icon name="copy" size="15" /><span>{{ __('ui.outbox.copy') }}</span>
                </button>
                <a class="btn ghost small" href="{{ $message->smsLink() }}">
                    <x-icon name="sms" size="15" />{{ __('ui.outbox.send_sms') }}
                </a>
                <a class="btn ghost small" href="{{ $message->viberLink() }}">
                    <x-icon name="chat" size="15" />{{ __('ui.outbox.send_viber') }}
                </a>
                <form method="post" action="{{ route('outbox.sent', $message) }}">
                    @csrf
                    <button class="btn small"><x-icon name="check" size="15" />{{ __('ui.outbox.mark_sent') }}</button>
                </form>
            </div>
        @elseif ($message->status === 'failed')
            <form method="post" action="{{ route('outbox.retry', $message) }}">
                @csrf
                <button class="btn small"><x-icon name="retry" size="15" />{{ __('ui.outbox.retry') }}</button>
            </form>
        @endif

        <div class="panel-foot">
            @if ($message->installation)
                <a class="btn ghost small" href="{{ route('installations.show', $message->installation) }}">
                    <x-icon name="book" size="15" />{{ $message->installation->unitName() }}
                </a>
            @endif
            <span class="grow"></span>
            <form method="post" action="{{ route('outbox.destroy', $message) }}">
                @csrf @method('DELETE')
                <button class="linkbtn">{{ __('ui.outbox.discard') }}</button>
            </form>
        </div>
    </div>
@empty
    <div class="panel">
        <div class="empty">
            <x-icon name="outbox" size="40" />
            <p>{{ __('ui.outbox.none') }}</p>
        </div>
    </div>
@endforelse

{{ $messages->withQueryString()->links() }}
@endsection

@push('scripts')
<script>
document.querySelectorAll('[data-copy]').forEach((btn) => {
  btn.addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(btn.dataset.copy); }
    catch (e) {
      const ta = document.createElement('textarea');
      ta.value = btn.dataset.copy; document.body.appendChild(ta); ta.select();
      document.execCommand('copy'); ta.remove();
    }
    const label = btn.querySelector('span') || btn;
    const original = label.textContent;
    label.textContent = @json(__('ui.show.copied'));
    setTimeout(() => { label.textContent = original; }, 1500);
  });
});
</script>
@endpush
