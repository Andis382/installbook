<?php

namespace App\Support;

/**
 * Serial numbers are read off dirty metal plates in bad light, by a person or by a camera,
 * and then typed back in months later by someone who half-remembers them. The characters
 * that get confused are always the same ones.
 *
 * The serial is stored exactly as it was typed, because that is what gets quoted in a
 * warranty claim. This produces a second, folded copy used only for searching, so that
 * looking for "0158" finds a unit recorded as "O1S8".
 */
class Serial
{
    private const FOLD = [
        'O' => '0', 'Q' => '0', 'D' => '0',
        'I' => '1', 'L' => '1',
        'S' => '5',
        'B' => '8',
        'Z' => '2',
        'G' => '6',
    ];

    public static function key(?string $serial): ?string
    {
        if ($serial === null) {
            return null;
        }

        $value = strtoupper(preg_replace('/[^A-Za-z0-9]+/', '', $serial) ?? '');

        if ($value === '') {
            return null;
        }

        return strtr($value, self::FOLD);
    }
}
