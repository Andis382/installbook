<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class Reminder extends Model
{
    use HasFactory;

    public const KIND_SERVICE_DUE = 'service_due';
    public const KIND_WARRANTY_ENDING = 'warranty_ending';

    public const AUDIENCE_CUSTOMER = 'customer';
    public const AUDIENCE_INSTALLER = 'installer';

    public const STATUS_PENDING = 'pending';
    public const STATUS_FIRED = 'fired';
    public const STATUS_CANCELLED = 'cancelled';
    public const STATUS_SKIPPED = 'skipped';

    protected $fillable = [
        'user_id', 'installation_id', 'kind', 'audience', 'fire_on',
        'status', 'message_id', 'fired_at', 'reason',
    ];

    protected function casts(): array
    {
        return [
            'fire_on' => 'date',
            'fired_at' => 'datetime',
        ];
    }

    public function installation(): BelongsTo
    {
        return $this->belongsTo(Installation::class);
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function message(): BelongsTo
    {
        return $this->belongsTo(Message::class);
    }

    public function kindLabel(): string
    {
        return __('reminder.'.$this->kind);
    }
}
