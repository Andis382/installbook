<?php

namespace App\Services\Messaging\Drivers;

use App\Models\Message;
use App\Services\Messaging\Contracts\WhatsAppDriver;

/**
 * The default, and the reason this product works on day one.
 *
 * Nothing is sent by a server. The message is written, translated and queued, and the
 * installer taps it: his own WhatsApp opens with the text already typed, addressed to
 * his own customer, from his own number. No Meta Business account, no template approval,
 * no per-conversation fee, and the customer gets a message from a number they recognise.
 *
 * The cost is that someone has to press send. For a one-man business with a handful of
 * reminders a week, that is a smaller price than a month of approvals.
 */
class ManualDriver implements WhatsAppDriver
{
    public function send(Message $message): void
    {
        // Left queued on purpose: the installer marks it sent from the outbox after tapping.
        if ($message->status !== Message::STATUS_QUEUED) {
            return;
        }

        $message->forceFill(['driver' => $this->name()])->save();
    }

    public function name(): string
    {
        return 'manual';
    }

    public function requiresInstallerAction(): bool
    {
        return true;
    }
}
