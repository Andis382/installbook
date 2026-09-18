<?php

namespace App\Models;

use App\Support\Phone;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;
use Illuminate\Database\Eloquent\Relations\HasMany;

class Customer extends Model
{
    use HasFactory;

    protected $fillable = [
        'user_id', 'name', 'phone', 'address', 'city', 'lat', 'lng',
        'locale', 'notes', 'messaging_consent_at', 'consent_text',
        'consent_method', 'opted_out_at',
    ];

    protected function casts(): array
    {
        return [
            'messaging_consent_at' => 'datetime',
            'opted_out_at' => 'datetime',
            'lat' => 'float',
            'lng' => 'float',
        ];
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function installations(): HasMany
    {
        return $this->hasMany(Installation::class);
    }

    /**
     * A message may only be produced for this customer if they agreed and have not
     * opted out. This is checked in one place so it cannot be forgotten at a call site.
     */
    public function canBeMessaged(): bool
    {
        return $this->messaging_consent_at !== null && $this->opted_out_at === null;
    }

    public function displayName(): string
    {
        return $this->name ?: Phone::pretty($this->phone);
    }

    public function label(): string
    {
        return $this->name
            ? $this->name.' · '.Phone::pretty($this->phone)
            : Phone::pretty($this->phone);
    }
}
