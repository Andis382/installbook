<?php

namespace App\Services\Messaging\Contracts;

use App\Models\Message;

interface WhatsAppDriver
{
    /**
     * Try to deliver the message. Implementations must mutate and save $message with
     * the outcome, and must never throw for an ordinary delivery failure — a failure
     * is a status on the row, not an exception that loses the message.
     */
    public function send(Message $message): void;

    /** Identifier stored on each message row. */
    public function name(): string;

    /**
     * True when the installer still has to press something for the message to leave.
     * The interface uses this to decide whether to show a "send" button or a receipt.
     */
    public function requiresInstallerAction(): bool;
}
