<?php

return [
    /*
     | The exact wording shown to a customer when permission is asked for, and the exact
     | wording stored on their record. It names the business, says what will be sent, and
     | says how to stop. It is stored per customer as text rather than as a reference,
     | because what matters later is what this person actually agreed to.
     */
    'statement' => ':business may send me a warranty card for this unit and a reminder when its service is due. I can stop these messages at any time from the link in the message.',

    'ask' => 'Ask the customer',
    'short' => 'They agree to receive the card and service reminders',
    'why' => 'Nothing is sent to anyone who has not agreed. Without this, the card is still created and you can hand over the link yourself.',
];
