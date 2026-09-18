<?php

namespace App\Services\Messaging\Drivers;

use App\Models\Message;
use App\Services\Messaging\Contracts\WhatsAppDriver;
use Illuminate\Support\Facades\Log;

/**
 * Writes the message to the log and marks it sent. For tests and for watching the
 * reminder engine work without messaging a real person.
 */
class LogDriver implements WhatsAppDriver
{
    public function send(Message $message): void
    {
        Log::info('[installbook] whatsapp message', [
            'to' => $message->to_phone,
            'template' => $message->template_key,
            'locale' => $message->locale,
            'body' => $message->body,
        ]);

        $message->forceFill([
            'driver' => $this->name(),
            'status' => Message::STATUS_SENT,
            'sent_at' => now(),
            'provider_message_id' => 'log-'.$message->id,
        ])->save();
    }

    public function name(): string
    {
        return 'log';
    }

    public function requiresInstallerAction(): bool
    {
        return false;
    }
}
