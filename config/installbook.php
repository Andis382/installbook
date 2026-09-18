<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Messaging
    |--------------------------------------------------------------------------
    |
    | "manual" is the default and needs no accounts, no approvals and no fees: the
    | app writes the message and the installer taps it open in his own WhatsApp.
    | "log" writes to the log instead, for tests and demos.
    | "cloud_api" sends through the WhatsApp Cloud API, which needs a Meta Business
    | account, a verified number and approved templates for anything outside the
    | 24-hour customer service window.
    |
    */

    'messaging' => [
        'driver' => env('INSTALLBOOK_MESSAGING_DRIVER', 'manual'),

        'cloud_api' => [
            'base_url' => env('WHATSAPP_API_BASE', 'https://graph.facebook.com'),
            'version' => env('WHATSAPP_API_VERSION', 'v21.0'),
            'phone_number_id' => env('WHATSAPP_PHONE_NUMBER_ID'),
            'token' => env('WHATSAPP_TOKEN'),
            'timeout' => (int) env('WHATSAPP_TIMEOUT', 15),

            /*
             | Approved template names, per message. A template is required to open a
             | conversation; plain text only reaches a customer who messaged you in the
             | last 24 hours. Leave a key empty to fall back to plain text.
             |
             | Each template is expected to take a single body parameter {{1}} which
             | receives the whole composed message, so the wording still lives in this
             | app's language files.
             */
            'templates' => [
                'card' => env('WHATSAPP_TEMPLATE_CARD'),
                'service_due' => env('WHATSAPP_TEMPLATE_SERVICE_DUE'),
                'warranty_ending' => env('WHATSAPP_TEMPLATE_WARRANTY_ENDING'),
                'booking_ack' => env('WHATSAPP_TEMPLATE_BOOKING_ACK'),
            ],
        ],
    ],

    /*
    |--------------------------------------------------------------------------
    | Serial plate reading
    |--------------------------------------------------------------------------
    |
    | Optional. "none" means the installer types the three fields, which always works.
    | "anthropic" reads the photographed data plate with a vision model and pre-fills
    | brand, model and serial, which the installer then confirms.
    |
    */

    'ocr' => [
        'driver' => env('INSTALLBOOK_OCR_DRIVER', 'none'),
        'model' => env('INSTALLBOOK_OCR_MODEL', 'claude-opus-5'),
        'max_tokens' => (int) env('INSTALLBOOK_OCR_MAX_TOKENS', 4096),
        'api_key' => env('ANTHROPIC_API_KEY'),
        'timeout' => (int) env('INSTALLBOOK_OCR_TIMEOUT', 40),
    ],

    /*
    |--------------------------------------------------------------------------
    | Defaults
    |--------------------------------------------------------------------------
    */

    'defaults' => [
        'country_prefix' => env('INSTALLBOOK_COUNTRY_PREFIX', '355'),
        'locale' => env('INSTALLBOOK_DEFAULT_LOCALE', 'sq'),
        'timezone' => env('INSTALLBOOK_DEFAULT_TIMEZONE', 'Europe/Tirane'),
        'currency' => env('INSTALLBOOK_CURRENCY', 'EUR'),

        /*
         | The guarantee a buyer has by law, in months from delivery. In the EU this is a
         | minimum of 24 months under Directive (EU) 2019/771, it is owed by the seller and
         | it is unconditional. It is shown on the card next to the manufacturer warranty so
         | the two are never confused, and it is never used in a reminder as leverage.
         | Set to 0 in markets where no statutory guarantee applies.
         */
        'statutory_warranty_months' => (int) env('INSTALLBOOK_STATUTORY_MONTHS', 24),
    ],

    /*
    | Locales offered in the interface and for customer messages.
    */
    'locales' => [
        'sq' => 'Shqip',
        'en' => 'English',
    ],

    /*
    | Uploaded photos. Plates are photographed in bad light, so the limit is generous.
    */
    'uploads' => [
        'max_kb' => (int) env('INSTALLBOOK_MAX_UPLOAD_KB', 8192),
        'disk' => env('INSTALLBOOK_UPLOAD_DISK', 'public'),
    ],
];
