<?php

namespace App\Support;

use Illuminate\Http\UploadedFile;
use Illuminate\Support\Facades\Storage;

class Uploads
{
    /** Store a photo and return its path on the configured disk, or null. */
    public static function store(?UploadedFile $file, string $folder): ?string
    {
        if (! $file || ! $file->isValid()) {
            return null;
        }

        return $file->store($folder, config('installbook.uploads.disk'));
    }

    public static function url(?string $path): ?string
    {
        if (! $path) {
            return null;
        }

        return Storage::disk(config('installbook.uploads.disk'))->url($path);
    }

    public static function absolutePath(?string $path): ?string
    {
        if (! $path) {
            return null;
        }

        $disk = Storage::disk(config('installbook.uploads.disk'));

        return method_exists($disk, 'path') ? $disk->path($path) : null;
    }

    public static function delete(?string $path): void
    {
        if ($path) {
            Storage::disk(config('installbook.uploads.disk'))->delete($path);
        }
    }
}
