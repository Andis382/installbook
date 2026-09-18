<?php

namespace App\Support;

/**
 * Phone numbers are the identity of a customer here, so they have to be stored in
 * one shape. This is deliberately not a full libphonenumber: it normalises to E.164
 * using a default country prefix, and leaves anything it cannot understand alone.
 */
class Phone
{
    /** Country prefixes we can expand a local number into, keyed by ISO code. */
    public const PREFIXES = [
        'AL' => '355',
        'XK' => '383',
        'MK' => '389',
        'ME' => '382',
        'RS' => '381',
        'GR' => '30',
        'IT' => '39',
        'DE' => '49',
        'UK' => '44',
        'US' => '1',
    ];

    /** National trunk prefixes to strip before prepending a country code. */
    private const TRUNK = '0';

    public static function normalize(?string $raw, string $defaultPrefix = '355'): ?string
    {
        if ($raw === null) {
            return null;
        }

        $value = trim($raw);
        if ($value === '') {
            return null;
        }

        $hadPlus = str_starts_with($value, '+') || str_starts_with($value, '00');
        $digits = preg_replace('/\D+/', '', $value) ?? '';

        if ($digits === '') {
            return null;
        }

        if (str_starts_with($value, '00')) {
            $digits = substr($digits, 2);
        }

        if (! $hadPlus) {
            // A local number: drop the trunk zero and prepend the installer's country.
            if (str_starts_with($digits, self::TRUNK)) {
                $digits = ltrim($digits, self::TRUNK);
            }
            if (! self::startsWithKnownPrefix($digits)) {
                $digits = $defaultPrefix.$digits;
            }
        }

        return '+'.$digits;
    }

    private static function startsWithKnownPrefix(string $digits): bool
    {
        foreach (self::PREFIXES as $prefix) {
            if (str_starts_with($digits, $prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Digits only, with no plus and no leading zero: the shape wa.me wants.
     * An empty result is meaningful — wa.me with no number opens the contact picker.
     */
    public static function digits(?string $phone): string
    {
        return preg_replace('/\D+/', '', (string) $phone) ?? '';
    }

    /** Light grouping for display: +355 69 123 4567 */
    public static function pretty(?string $phone): string
    {
        if (! $phone) {
            return '';
        }

        $digits = self::digits($phone);
        if ($digits === '') {
            return (string) $phone;
        }

        foreach (self::PREFIXES as $prefix) {
            if (! str_starts_with($digits, $prefix)) {
                continue;
            }

            $rest = substr($digits, strlen($prefix));

            // A nine-digit national number is a mobile almost everywhere in the region and
            // is read as 69 123 4567. Anything else is grouped in threes, except that a
            // lone trailing digit joins the group before it rather than dangling.
            if (strlen($rest) === 9) {
                $groups = [substr($rest, 0, 2), substr($rest, 2, 3), substr($rest, 5)];
            } else {
                $groups = str_split($rest, 3) ?: [];
                if (count($groups) > 1 && strlen(end($groups)) === 1) {
                    $last = array_pop($groups);
                    $groups[count($groups) - 1] .= $last;
                }
            }

            return '+'.$prefix.' '.implode(' ', array_filter($groups));
        }

        return '+'.$digits;
    }

    public static function isPlausible(?string $phone): bool
    {
        $digits = self::digits($phone);

        return strlen($digits) >= 8 && strlen($digits) <= 15;
    }
}
