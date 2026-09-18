<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class BookingRequest extends Model
{
    use HasFactory;

    public const STATUS_NEW = 'new';
    public const STATUS_SCHEDULED = 'scheduled';
    public const STATUS_DONE = 'done';
    public const STATUS_DECLINED = 'declined';

    public const WINDOWS = ['this_week', 'next_week', 'this_month', 'morning', 'afternoon'];

    protected $fillable = [
        'user_id', 'installation_id', 'preferred_window', 'note',
        'contact_phone', 'status', 'scheduled_for', 'resolved_at',
    ];

    protected function casts(): array
    {
        return [
            'scheduled_for' => 'date',
            'resolved_at' => 'datetime',
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

    public function windowLabel(): ?string
    {
        return $this->preferred_window ? __('booking.window.'.$this->preferred_window) : null;
    }

    public function statusLabel(): string
    {
        return __('booking.status.'.$this->status);
    }
}
