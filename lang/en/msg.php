<?php

/*
 | Every word a customer receives.
 |
 | Three rules apply to everything in this file and to any translation of it:
 |
 |  1. Nothing promotional. No offers, no prices, no discounts, no "hurry". A reminder
 |     about a specific machine is a utility message; an advert is not, and dressing one
 |     as the other is both dishonest and, on WhatsApp, several times more expensive.
 |  2. Never imply that a guarantee is at risk unless it truly is. The buyer's statutory
 |     guarantee cannot be voided by a missed service, and the separate manufacturer
 |     warranty is only mentioned when this unit is recorded as requiring annual service.
 |  3. Every message says who it is from and how to stop it.
 */

return [

    'card' => <<<'TXT'
    :installer

    Your :type is registered: :unit
    Serial: :serial
    Installed: :installed
    Warranty until: :warranty

    Keep this link. It is your proof of what was installed and when, and it is where you can ask for a service:
    :link
    TXT,

    'service_due_soon' => <<<'TXT'
    :installer

    Your :type (:unit) is due for its service on :due.

    To arrange it, reply to this message or open:
    :link
    TXT,

    'service_due_now' => <<<'TXT'
    :installer

    Your :type (:unit) is due for its service today.

    To arrange it, reply to this message or open:
    :link
    TXT,

    'warranty_ending' => <<<'TXT'
    :installer

    The manufacturer warranty on your :unit ends on :warranty.

    Your record stays available here:
    :link
    TXT,

    'booking_ack' => <<<'TXT'
    :installer

    We have your service request for your :unit and will be in touch to agree a time.
    TXT,

    // Appended only when this unit is recorded as one whose manufacturer warranty
    // genuinely depends on a logged annual service.
    'service_keeps_warranty' => 'The manufacturer requires a yearly service on this unit to keep its warranty, which runs to :warranty. This does not affect your statutory rights as a buyer.',

    'stop_line' => 'To stop these messages: :link',
];
