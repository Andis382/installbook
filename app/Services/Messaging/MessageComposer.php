<?php

namespace App\Services\Messaging;

use App\Models\BookingRequest;
use App\Models\Installation;
use App\Models\Message;
use Illuminate\Support\Carbon;

/**
 * Builds the text of every message, in the customer's own language.
 *
 * Two rules are enforced here rather than left to whoever writes copy later:
 * a message never claims the warranty will be voided unless the manufacturer really
 * does require a logged annual service on that unit, and the warranty line is only
 * included when there is a warranty date to state.
 */
class MessageComposer
{
    public function __construct(private readonly CardLinkBuilder $links) {}

    public function card(Installation $installation): string
    {
        $locale = $this->localeFor($installation);

        return $this->render('card', $locale, [
            'installer' => $installation->user->displayName(),
            'unit' => $installation->unitName(),
            'type' => $this->typeLabel($installation, $locale),
            'serial' => $installation->serial ?: '—',
            'installed' => $this->date($installation->installed_on),
            'warranty' => $installation->warranty_expires_on ? $this->date($installation->warranty_expires_on) : null,
            'link' => $this->links->for($installation),
        ]);
    }

    public function serviceDue(Installation $installation, bool $dueToday): string
    {
        $locale = $this->localeFor($installation);
        $key = $dueToday ? 'service_due_now' : 'service_due_soon';

        $body = $this->render($key, $locale, [
            'installer' => $installation->user->displayName(),
            'unit' => $installation->unitName(),
            'type' => $this->typeLabel($installation, $locale),
            'due' => $this->date($installation->next_service_due_on),
            'link' => $this->links->for($installation),
        ]);

        // Only say the warranty depends on the service when it actually does.
        if ($installation->service_required_for_warranty && $installation->warrantyIsActive()) {
            $body .= "\n\n".__('msg.service_keeps_warranty', [
                'warranty' => $this->date($installation->warranty_expires_on),
            ], $locale);
        }

        return $body;
    }

    public function warrantyEnding(Installation $installation): string
    {
        $locale = $this->localeFor($installation);

        return $this->render('warranty_ending', $locale, [
            'installer' => $installation->user->displayName(),
            'unit' => $installation->unitName(),
            'warranty' => $this->date($installation->warranty_expires_on),
            'link' => $this->links->for($installation),
        ]);
    }

    public function bookingAck(BookingRequest $booking): string
    {
        $installation = $booking->installation;
        $locale = $this->localeFor($installation);

        return $this->render('booking_ack', $locale, [
            'installer' => $installation->user->displayName(),
            'unit' => $installation->unitName(),
            'phone' => $installation->user->phone ?: '',
        ]);
    }

    public function templateKeyFor(string $composed): string
    {
        return match ($composed) {
            'card' => Message::TEMPLATE_CARD,
            'warranty_ending' => Message::TEMPLATE_WARRANTY_ENDING,
            'booking_ack' => Message::TEMPLATE_BOOKING_ACK,
            default => Message::TEMPLATE_SERVICE_DUE,
        };
    }

    public function localeFor(Installation $installation): string
    {
        return $installation->customer?->locale
            ?: $installation->user->locale
            ?: config('app.locale');
    }

    private function typeLabel(Installation $installation, string $locale): string
    {
        return __($installation->type()->labelKey(), [], $locale);
    }

    private function render(string $key, string $locale, array $data): string
    {
        $data = array_map(fn ($v) => $v === null ? '' : $v, $data);

        $body = __('msg.'.$key, $data, $locale);

        // Collapse the blank line left behind when an optional value was empty.
        return trim(preg_replace("/\n{3,}/", "\n\n", $body));
    }

    private function date(?Carbon $date): string
    {
        return $date?->format('d/m/Y') ?? '';
    }
}
