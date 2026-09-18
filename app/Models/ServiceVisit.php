<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class ServiceVisit extends Model
{
    use HasFactory;

    public const KIND_INSTALL = 'install';
    public const KIND_SERVICE = 'service';
    public const KIND_REPAIR = 'repair';
    public const KIND_INSPECTION = 'inspection';

    public const KINDS = [self::KIND_INSTALL, self::KIND_SERVICE, self::KIND_REPAIR, self::KIND_INSPECTION];

    protected $fillable = [
        'user_id', 'installation_id', 'kind', 'performed_on',
        'notes', 'photo_path', 'price_cents', 'currency',
    ];

    protected function casts(): array
    {
        return ['performed_on' => 'date'];
    }

    public function installation(): BelongsTo
    {
        return $this->belongsTo(Installation::class);
    }

    public function user(): BelongsTo
    {
        return $this->belongsTo(User::class);
    }

    public function kindLabel(): string
    {
        return __('visit.'.$this->kind);
    }

    public function priceLabel(): ?string
    {
        if ($this->price_cents === null) {
            return null;
        }

        return number_format($this->price_cents / 100, 2).' '.$this->currency;
    }
}
