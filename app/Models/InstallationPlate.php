<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class InstallationPlate extends Model
{
    use HasFactory;

    public const ROLE_MAIN = 'main';
    public const ROLE_INDOOR = 'indoor';
    public const ROLE_OUTDOOR = 'outdoor';
    public const ROLE_INVERTER = 'inverter';
    public const ROLE_PANEL = 'panel';
    public const ROLE_OTHER = 'other';

    public const ROLES = [
        self::ROLE_INDOOR, self::ROLE_OUTDOOR, self::ROLE_INVERTER,
        self::ROLE_PANEL, self::ROLE_MAIN, self::ROLE_OTHER,
    ];

    protected $fillable = [
        'installation_id', 'role', 'brand', 'model', 'serial', 'photo_path', 'note',
    ];

    public function installation(): BelongsTo
    {
        return $this->belongsTo(Installation::class);
    }

    public function roleLabel(): string
    {
        return __('plate.role.'.$this->role);
    }

    public function unitName(): string
    {
        $parts = array_filter([$this->brand, $this->model]);

        return $parts ? implode(' ', $parts) : $this->roleLabel();
    }
}
