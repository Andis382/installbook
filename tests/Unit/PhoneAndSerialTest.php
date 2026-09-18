<?php

namespace Tests\Unit;

use App\Support\Phone;
use App\Support\Serial;
use PHPUnit\Framework\TestCase;

class PhoneAndSerialTest extends TestCase
{
    /**
     * Every way an installer might type the same Albanian mobile number has to end up as
     * one string, because that string is the identity of a customer.
     */
    public function test_the_same_number_written_five_ways_normalises_identically(): void
    {
        $expected = '+355691234567';

        foreach ([
            '069 123 4567',
            '+355 69 123 4567',
            '0035569-123-4567',
            '(069) 1234567',
            '355691234567',
        ] as $input) {
            $this->assertSame($expected, Phone::normalize($input, '355'), "failed for: {$input}");
        }
    }

    public function test_a_foreign_number_keeps_its_own_country_code(): void
    {
        $this->assertSame('+393331234567', Phone::normalize('+39 333 1234567', '355'));
        $this->assertSame('+393331234567', Phone::normalize('0039 333 1234567', '355'));
    }

    public function test_empty_input_is_null_rather_than_a_broken_number(): void
    {
        $this->assertNull(Phone::normalize(null));
        $this->assertNull(Phone::normalize(''));
        $this->assertNull(Phone::normalize('   '));
        $this->assertNull(Phone::normalize('abc'));
    }

    public function test_digits_strips_everything_a_deep_link_cannot_carry(): void
    {
        $this->assertSame('355691234567', Phone::digits('+355 69 123 4567'));
        $this->assertSame('', Phone::digits(null));
    }

    public function test_a_number_is_displayed_the_way_it_is_read_aloud(): void
    {
        $this->assertSame('+355 69 123 4567', Phone::pretty('+355691234567'));
        $this->assertSame('+39 333 123 4567', Phone::pretty('+393331234567'));
        $this->assertSame('', Phone::pretty(null));
    }

    public function test_plausibility_rejects_obviously_wrong_lengths(): void
    {
        $this->assertTrue(Phone::isPlausible('+355691234567'));
        $this->assertFalse(Phone::isPlausible('+3551'));
        $this->assertFalse(Phone::isPlausible('+3556912345678901234'));
    }

    /**
     * The point of the folded key: a serial read as "O1S8" from a dirty plate is still
     * found by someone searching for what they think they saw, "0158".
     */
    public function test_confusable_characters_fold_to_one_search_key(): void
    {
        $this->assertSame(Serial::key('O1S8'), Serial::key('0158'));
        $this->assertSame(Serial::key('IL0O'), Serial::key('1100'));
        $this->assertSame(Serial::key('BZG'), Serial::key('826'));
    }

    public function test_punctuation_and_case_do_not_affect_the_key(): void
    {
        $this->assertSame(Serial::key('va24-118837/2025'), Serial::key('VA 24 118837 2025'));
    }

    public function test_an_empty_serial_has_no_key(): void
    {
        $this->assertNull(Serial::key(null));
        $this->assertNull(Serial::key('   '));
        $this->assertNull(Serial::key('---'));
    }
}
