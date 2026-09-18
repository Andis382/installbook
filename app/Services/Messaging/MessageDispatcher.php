<?php

namespace App\Services\Messaging;

use App\Models\BookingRequest;
use App\Models\Customer;
use App\Models\Installation;
use App\Models\Message;
use App\Services\Messaging\Contracts\WhatsAppDriver;

/**
 * The only place a message is created. Everything else asks for one here, which means
 * consent, language and de-duplication are checked once instead of at every call site.
 */
class MessageDispatcher
{
    public function __construct(
        private readonly MessageComposer $composer,
        private readonly CardLinkBuilder $links,
        private readonly WhatsAppDriver $driver,
    ) {}

    public function driverName(): string
    {
        return $this->driver->name();
    }

    public function requiresInstallerAction(): bool
    {
        return $this->driver->requiresInstallerAction();
    }

    public function sendCard(Installation $installation): ?Message
    {
        return $this->queue(
            $installation,
            Message::TEMPLATE_CARD,
            $this->composer->card($installation),
        );
    }

    public function sendServiceDue(Installation $installation, bool $dueToday): ?Message
    {
        return $this->queue(
            $installation,
            Message::TEMPLATE_SERVICE_DUE,
            $this->composer->serviceDue($installation, $dueToday),
        );
    }

    public function sendWarrantyEnding(Installation $installation): ?Message
    {
        return $this->queue(
            $installation,
            Message::TEMPLATE_WARRANTY_ENDING,
            $this->composer->warrantyEnding($installation),
        );
    }

    public function sendBookingAck(BookingRequest $booking): ?Message
    {
        return $this->queue(
            $booking->installation,
            Message::TEMPLATE_BOOKING_ACK,
            $this->composer->bookingAck($booking),
        );
    }

    /**
     * Returns the message row, or null when nothing may be sent to this customer.
     * A refusal is recorded as a skipped row so the installer can see why it is quiet.
     */
    public function queue(Installation $installation, string $templateKey, string $body): ?Message
    {
        $customer = $installation->customer;

        if (! $customer) {
            return null;
        }

        $locale = $this->composer->localeFor($installation);

        $message = new Message([
            'user_id' => $installation->user_id,
            'customer_id' => $customer->id,
            'installation_id' => $installation->id,
            'template_key' => $templateKey,
            'locale' => $locale,
            'to_phone' => $customer->phone,
            'body' => $body,
            'link' => $this->links->for($installation),
            'channel' => 'whatsapp',
            'driver' => $this->driver->name(),
            'status' => Message::STATUS_QUEUED,
        ]);

        if ($reason = $this->refusalReason($customer)) {
            $message->status = Message::STATUS_SKIPPED;
            $message->error = $reason;
            $message->save();

            return $message;
        }

        $message->save();
        $this->driver->send($message);

        return $message->refresh();
    }

    /** Re-run the driver for a message that failed or is waiting. */
    public function retry(Message $message): Message
    {
        if ($message->status === Message::STATUS_FAILED) {
            $message->forceFill(['status' => Message::STATUS_QUEUED, 'error' => null])->save();
        }

        $this->driver->send($message);

        return $message->refresh();
    }

    private function refusalReason(Customer $customer): ?string
    {
        if ($customer->opted_out_at !== null) {
            return 'customer_opted_out';
        }

        if ($customer->messaging_consent_at === null) {
            return 'no_messaging_consent';
        }

        return null;
    }
}
