@extends('layouts.app')
@section('title', __('ui.outbox.title'))

@section('content')
<h1>{{ __('ui.outbox.title') }}</h1>

<p class="muted">
    @if ($requiresTap)
        {{ __('ui.outbox.manual_explain') }}
    @else
        {{ __('ui.outbox.auto_explain', ['driver' => $driver]) }}
    @endif
</p>

<div class="chips">
    @foreach (['queued', 'sent', 'failed', 'skipped'] as $key)
        <a class="chip {{ $status === $key ? 'on' : '' }}" href="{{ route('outbox.index', ['status' => $key]) }}">
            {{ __('message.status.'.$key) }} ({{ $counts[$key] }})
        </a>
    @endforeach
</div>

@forelse ($messages as $message)
    <div class="card">
        <div class="row between">
            <span>
                <strong>{{ $message->templateLabel() }}</strong>
                <br><span class="small muted">
                    {{ $message->customer?->displayName() ?? \App\Support\Phone::pretty($message->to_phone) }}
                    · {{ $message->created_at->format('d/m H:i') }}
                    · {{ strtoupper($message->locale) }}
                </span>
            </span>
            <span class="pill {{ $message->status === 'sent' ? 'ok' : ($message->status === 'failed' ? 'danger' : ($message->status === 'skipped' ? 'quiet' : 'warn')) }}">
                {{ $message->statusLabel() }}
            </span>
        </div>

        <div class="msgbody">{{ $message->body }}</div>

        @if ($message->status === 'skipped')
            <p class="small muted">{{ __('ui.outbox.skipped_reason', ['reason' => __('ui.outbox.reason.'.$message->error, [], null) ?: $message->error]) }}</p>
        @elseif ($message->error)
            <p class="small" style="color:var(--danger)">{{ $message->error }}</p>
        @endif

        @if ($message->status === 'queued')
            <a class="btn wa block" href="{{ $message->whatsappLink() }}" target="_blank" rel="noopener">💬 {{ __('ui.outbox.send') }}</a>
            <div class="row tight" style="margin-top:8px">
                <button type="button" class="btn ghost small" data-copy="{{ $message->body }}">{{ __('ui.outbox.copy') }}</button>
                <a class="btn ghost small" href="{{ $message->smsLink() }}">{{ __('ui.outbox.send_sms') }}</a>
                <a class="btn ghost small" href="{{ $message->viberLink() }}">{{ __('ui.outbox.send_viber') }}</a>
                <form method="post" action="{{ route('outbox.sent', $message) }}">
                    @csrf <button class="btn small">✓ {{ __('ui.outbox.mark_sent') }}</button>
                </form>
            </div>
        @elseif ($message->status === 'failed')
            <form method="post" action="{{ route('outbox.retry', $message) }}">
                @csrf <button class="btn small">{{ __('ui.outbox.retry') }}</button>
            </form>
        @endif

        <div class="row tight" style="margin-top:8px">
            @if ($message->installation)
                <a class="small" href="{{ route('installations.show', $message->installation) }}">{{ $message->installation->unitName() }}</a>
            @endif
            <span class="grow"></span>
            <form method="post" action="{{ route('outbox.destroy', $message) }}">
                @csrf @method('DELETE')
                <button class="linkbtn small">{{ __('ui.outbox.discard') }}</button>
            </form>
        </div>
    </div>
@empty
    <div class="empty"><span class="big">📭</span>{{ __('ui.outbox.none') }}</div>
@endforelse

{{ $messages->links() }}
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
    const original = btn.textContent;
    btn.textContent = @json(__('ui.show.copied'));
    setTimeout(() => { btn.textContent = original; }, 1500);
  });
});
</script>
@endpush
