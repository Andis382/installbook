<?php

namespace App\Http\Controllers;

use App\Http\Controllers\Concerns\ScopesToInstaller;
use App\Models\Message;
use App\Services\Messaging\MessageDispatcher;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

/**
 * With the default driver nothing leaves the server: this page is where the installer
 * sees each written message and taps it into his own WhatsApp. It doubles as the record
 * of what was said to whom.
 */
class OutboxController extends Controller
{
    use ScopesToInstaller;

    public function __construct(private readonly MessageDispatcher $dispatcher) {}

    public function index(Request $request): View
    {
        $status = $request->query('status', Message::STATUS_QUEUED);

        $messages = $request->user()->messages()
            ->with(['customer', 'installation'])
            ->when(in_array($status, [Message::STATUS_QUEUED, Message::STATUS_SENT, Message::STATUS_FAILED, Message::STATUS_SKIPPED], true),
                fn ($q) => $q->where('status', $status))
            ->latest()
            ->paginate(20)
            ->withQueryString();

        return view('outbox.index', [
            'messages' => $messages,
            'status' => $status,
            'requiresTap' => $this->dispatcher->requiresInstallerAction(),
            'driver' => $this->dispatcher->driverName(),
            'counts' => [
                Message::STATUS_QUEUED => $request->user()->messages()->where('status', Message::STATUS_QUEUED)->count(),
                Message::STATUS_SENT => $request->user()->messages()->where('status', Message::STATUS_SENT)->count(),
                Message::STATUS_FAILED => $request->user()->messages()->where('status', Message::STATUS_FAILED)->count(),
                Message::STATUS_SKIPPED => $request->user()->messages()->where('status', Message::STATUS_SKIPPED)->count(),
            ],
        ]);
    }

    /** The installer tapped the WhatsApp link and sent it himself. */
    public function markSent(Request $request, Message $message): RedirectResponse
    {
        $this->mine($request, $message);

        $message->forceFill([
            'status' => Message::STATUS_SENT,
            'sent_at' => now(),
        ])->save();

        return back()->with('status', __('flash.message_marked_sent'));
    }

    public function retry(Request $request, Message $message): RedirectResponse
    {
        $this->mine($request, $message);

        $this->dispatcher->retry($message);

        return back()->with('status', __('flash.message_retried'));
    }

    public function destroy(Request $request, Message $message): RedirectResponse
    {
        $this->mine($request, $message);

        $message->delete();

        return back()->with('status', __('flash.message_discarded'));
    }
}
