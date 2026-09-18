<?php

namespace Tests\Unit;

use PHPUnit\Framework\TestCase;

/**
 * The promise this product makes about its own messages, enforced rather than described.
 *
 * Two constraints happen to be the same constraint. An honest reminder about one specific
 * machine is also, in WhatsApp's own categories, a "utility" message rather than
 * "marketing" — which is several times cheaper. Sales language would make the message both
 * dishonest and more expensive, so it is checked here against the shipped wording in every
 * language, and a translation that drifts fails the build.
 */
class MessageToneTest extends TestCase
{
    /** Words that turn a notice into an advert, in both shipped languages. */
    private const PROMOTIONAL = [
        'offer', 'discount', 'free', 'hurry', 'last chance', 'special', 'deal', 'save now',
        'ofertë', 'oferte', 'zbritje', 'falas', 'nxitoni', 'shpejt tani', 'promocion',
    ];

    /** Claims about losing cover that the message itself is not entitled to make. */
    private const SCARE = [
        'void', 'expire immediately', 'lose your warranty', 'warranty will be cancelled',
        'humbni garancinë', 'anulohet garancia', 'skadon menjëherë',
    ];

    private function locales(): array
    {
        return ['en', 'sq'];
    }

    private function messages(string $locale): array
    {
        $path = dirname(__DIR__, 2)."/lang/{$locale}/msg.php";
        $this->assertFileExists($path);

        $messages = include $path;
        $this->assertIsArray($messages);

        return $messages;
    }

    public function test_no_shipped_message_uses_promotional_language(): void
    {
        foreach ($this->locales() as $locale) {
            foreach ($this->messages($locale) as $key => $text) {
                $haystack = mb_strtolower(is_array($text) ? implode(' ', $text) : $text);
                foreach (self::PROMOTIONAL as $word) {
                    $this->assertStringNotContainsString(
                        $word,
                        $haystack,
                        "The {$locale} message '{$key}' contains promotional wording: '{$word}'."
                    );
                }
            }
        }
    }

    public function test_no_shipped_message_threatens_the_customer_with_losing_cover(): void
    {
        foreach ($this->locales() as $locale) {
            foreach ($this->messages($locale) as $key => $text) {
                $haystack = mb_strtolower(is_array($text) ? implode(' ', $text) : $text);
                foreach (self::SCARE as $phrase) {
                    $this->assertStringNotContainsString(
                        $phrase,
                        $haystack,
                        "The {$locale} message '{$key}' threatens the customer: '{$phrase}'."
                    );
                }
            }
        }
    }

    public function test_no_message_shouts(): void
    {
        foreach ($this->locales() as $locale) {
            foreach ($this->messages($locale) as $key => $text) {
                if (! is_string($text)) {
                    continue;
                }
                $this->assertStringNotContainsString('!', $text, "The {$locale} message '{$key}' uses an exclamation mark.");
            }
        }
    }

    public function test_every_language_ships_the_same_set_of_messages(): void
    {
        $reference = array_keys($this->messages('en'));

        foreach ($this->locales() as $locale) {
            $this->assertSame(
                $reference,
                array_keys($this->messages($locale)),
                "The {$locale} messages do not match the English set; a customer would receive a raw key."
            );
        }
    }

    public function test_the_warranty_sentence_is_only_available_as_an_addition(): void
    {
        // It must never be baked into the reminder itself: it is appended only when the
        // unit is recorded as one whose manufacturer warranty depends on a yearly service.
        foreach ($this->locales() as $locale) {
            $messages = $this->messages($locale);
            $this->assertArrayHasKey('service_keeps_warranty', $messages);

            foreach (['service_due_soon', 'service_due_now'] as $key) {
                $this->assertStringNotContainsStringIgnoringCase('garanc', $messages[$key], "{$locale}.{$key} mentions the warranty unconditionally.");
                $this->assertStringNotContainsStringIgnoringCase('warrant', $messages[$key], "{$locale}.{$key} mentions the warranty unconditionally.");
            }
        }
    }
}
