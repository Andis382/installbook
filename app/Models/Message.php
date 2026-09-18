<?php

namespace App\Models;

use App\Support\Phone;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Message extends Model
{
    use HasFactory;

    public const STATUS_QUEUED = 'queued';
    public const STATUS_SENT = 'sent';
    public const STATUS_FAILED = 'failed';
    public const STATUS_SKIPPED = 'skipped';

    public const TEMPLATE_CARD = 'card';
    public const TEMPLATE_SERVICE_DUE = 'service_due';
    public const TEMPLATE_WARRANTY_ENDING = 'warranty_ending';
    public const TEMPLATE_BOOKING_ACK = 'booking_ack';

    protected $fillable = [
        'user_id', 'customer_id', 'installation_id', 'template_key', 'locale',
        'to_phone', 'body', 'link', 'channel', 'driver', 'status',
        'provider_message_id', 'error', 'sent_at',
    ];

    protected function casts(): array
    {
        return ['sent_at' => 'datetime'];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function customer(): BelongsTo
    {
        return $this->belongsTo(Customer::class);
    }

    public function installation(): BelongsTo
    {
        return $this->belongsTo(Installation::class);
    }

    /**
     * The click-to-chat deep link. This is what makes the product work on day one:
     * the installer taps it and his own WhatsApp opens with the message already typed,
     * so nothing depends on a Meta Business account or template approval.
     */
    public function whatsappLink(): string
    {
        // rawurlencode, not urlencode: urlencode turns a space into "+", which some WhatsApp
        // clients show literally. Newlines become %0A, which is what makes the card readable.
        $text = rawurlencode($this->body);
        $digits = Phone::digits($this->to_phone);

        // With no number, WhatsApp opens its own contact picker with the text ready.
        return $digits === ''
            ? 'https://wa.me/?text='.$text
            : 'https://wa.me/'.$digits.'?text='.$text;
    }

    /** Viber is the everyday app in parts of the same region; same text, different rail. */
    public function viberLink(): string
    {
        return 'viber://forward?text='.rawurlencode($this->body);
    }

    public function smsLink(): string
    {
        return 'sms:'.$this->to_phone.'?body='.rawurlencode($this->body);
    }

    public function isPending(): bool
    {
        return $this->status === self::STATUS_QUEUED;
    }

    public function statusLabel(): string
    {
        return __('message.status.'.$this->status);
    }

    public function templateLabel(): string
    {
        return __('message.template.'.$this->template_key);
    }
}
