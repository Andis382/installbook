<?php

namespace App\Services\Plate;

use Anthropic\Client;
use Anthropic\Messages\Base64ImageSource;
use Anthropic\Messages\Base64ImageSource\MediaType;
use Anthropic\Messages\ImageBlockParam;
use Illuminate\Support\Facades\Log;
use Throwable;

/**
 * Reads a photographed data plate with a vision model and returns the three fields the
 * register actually needs. Optional: when it is off, or when it fails, the installer
 * types them instead, which is the path that always works.
 *
 * Deliberately conservative. The model is told to transcribe only what it can see and
 * to leave a field null rather than guess, because a confidently wrong serial number is
 * worse than an empty one: it is the thing a warranty claim will later turn on.
 */
class AnthropicPlateReader implements PlateReader
{
    private const SCHEMA = [
        'type' => 'object',
        'properties' => [
            'brand' => ['type' => ['string', 'null'], 'description' => 'Manufacturer exactly as printed, e.g. Vaillant, Daikin, Ariston.'],
            'model' => ['type' => ['string', 'null'], 'description' => 'Model or type designation as printed.'],
            'serial' => ['type' => ['string', 'null'], 'description' => 'Serial number. Transcribe character by character. Null if any character is unreadable.'],
            'extra_code' => ['type' => ['string', 'null'], 'description' => 'Any second identifier such as a GC number, PNC, E-number or product code.'],
            'capacity' => ['type' => ['string', 'null'], 'description' => 'Rated output as printed, e.g. "24 kW" or "12000 BTU".'],
            'manufactured_year' => ['type' => ['integer', 'null'], 'description' => 'Four digit year of manufacture if printed.'],
            'confidence' => ['type' => 'number', 'description' => 'How legible the plate was overall, 0 to 1.'],
            'raw_text' => ['type' => ['string', 'null'], 'description' => 'Every line of text visible on the plate, in order.'],
        ],
        'required' => ['brand', 'model', 'serial', 'extra_code', 'capacity', 'manufactured_year', 'confidence', 'raw_text'],
        'additionalProperties' => false,
    ];

    public function __construct(
        private readonly ?string $apiKey,
        private readonly string $model,
        private readonly int $maxTokens,
        private readonly int $timeout,
    ) {}

    public function isEnabled(): bool
    {
        return ! empty($this->apiKey);
    }

    public function read(string $absolutePath, ?string $applianceType = null): PlateReading
    {
        if (! $this->isEnabled()) {
            return PlateReading::empty('ocr_disabled');
        }

        if (! is_readable($absolutePath)) {
            return PlateReading::empty('file_unreadable');
        }

        $mediaType = $this->mediaType($absolutePath);
        if ($mediaType === null) {
            return PlateReading::empty('unsupported_image_type');
        }

        try {
            $client = new Client(apiKey: $this->apiKey);

            $message = $client->messages->create(
                model: $this->model,
                maxTokens: $this->maxTokens,
                system: $this->systemPrompt(),
                outputConfig: [
                    'effort' => 'low',
                    'format' => ['type' => 'json_schema', 'schema' => self::SCHEMA],
                ],
                messages: [[
                    'role' => 'user',
                    'content' => [
                        ImageBlockParam::with(
                            source: Base64ImageSource::with(
                                data: base64_encode((string) file_get_contents($absolutePath)),
                                mediaType: $mediaType,
                            ),
                        ),
                        ['type' => 'text', 'text' => $this->userPrompt($applianceType)],
                    ],
                ]],
            );
        } catch (Throwable $e) {
            Log::warning('[installbook] plate read failed', ['error' => $e->getMessage()]);

            return PlateReading::empty('request_failed');
        }

        foreach ($message->content as $block) {
            if (($block->type ?? null) === 'text') {
                return $this->parse($block->text);
            }
        }

        return PlateReading::empty('no_text_block');
    }

    private function parse(string $json): PlateReading
    {
        $data = json_decode($json, true);

        if (! is_array($data)) {
            return PlateReading::empty('unparseable_response');
        }

        $clean = fn (?string $v) => is_string($v) && trim($v) !== '' ? trim($v) : null;

        return new PlateReading(
            brand: $clean($data['brand'] ?? null),
            model: $clean($data['model'] ?? null),
            serial: $clean($data['serial'] ?? null),
            extraCode: $clean($data['extra_code'] ?? null),
            capacity: $clean($data['capacity'] ?? null),
            manufacturedYear: is_numeric($data['manufactured_year'] ?? null) ? (int) $data['manufactured_year'] : null,
            rawText: $clean($data['raw_text'] ?? null),
            confidence: is_numeric($data['confidence'] ?? null) ? (float) $data['confidence'] : null,
        );
    }

    private function systemPrompt(): string
    {
        return <<<'TXT'
        You transcribe data plates on domestic heating, cooling and water equipment.

        Transcribe only what is visibly printed. Never infer, complete or correct a value
        from what is plausible for that manufacturer. A serial number that is partly
        obscured, blurred or cut off must be returned as null, not as a best guess: this
        number is what a later warranty claim depends on, so an empty field is useful and
        a wrong field is harmful.

        Plates often carry several numbers. The serial is the one unique to the single
        unit, usually the longest and often next to "Ser. Nr.", "S/N", "Serial No",
        "Nr. serie" or a barcode. Product, article, GC, PNC and E-numbers identify the
        model rather than the unit: put those in extra_code.

        Labels may be in English, Italian, German, Turkish, Albanian or Greek.
        TXT;
    }

    private function userPrompt(?string $applianceType): string
    {
        $hint = $applianceType ? "The installer recorded this unit as: {$applianceType}." : '';

        return trim("This is a photograph of the data plate of a unit that was just installed. {$hint} Transcribe it.");
    }

    private function mediaType(string $path): ?MediaType
    {
        $mime = @mime_content_type($path) ?: '';

        return match ($mime) {
            'image/jpeg' => MediaType::IMAGE_JPEG,
            'image/png' => MediaType::IMAGE_PNG,
            'image/webp' => MediaType::IMAGE_WEBP,
            'image/gif' => MediaType::IMAGE_GIF,
            default => null,
        };
    }
}
