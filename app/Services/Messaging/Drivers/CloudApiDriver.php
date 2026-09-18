<?php

namespace App\Services\Messaging\Drivers;

use App\Models\Message;
use App\Services\Messaging\Contracts\WhatsAppDriver;
use App\Support\Phone;
use Illuminate\Support\Facades\Http;
use Throwable;

/**
 * WhatsApp Cloud API. Optional, and deliberately not the default.
 *
 * Two things are worth knowing before turning this on. A business can only send a
 * free-form text to someone who messaged it in the last 24 hours; outside that window
 * the message must use a template that Meta has approved in advance, which takes time
 * and can be rejected. And conversations are billed. The manual driver has neither
 * problem, which is why it stays the default.
 *
 * When a template name is configured for a message type, that template is used with the
 * composed text as its single body parameter, so the wording still lives in this app.
 * Otherwise a plain text message is attempted and will only arrive inside the window.
 */
class CloudApiDriver implements WhatsAppDriver
{
    public function __construct(private readonly array $config) {}

    public function send(Message $message): void
    {
        $phoneNumberId = $this->config['phone_number_id'] ?? null;
        $token = $this->config['token'] ?? null;

        if (! $phoneNumberId || ! $token) {
            $this->fail($message, 'WhatsApp Cloud API is selected but WHATSAPP_PHONE_NUMBER_ID or WHATSAPP_TOKEN is missing.');

            return;
        }

        $url = rtrim($this->config['base_url'], '/').'/'.$this->config['version'].'/'.$phoneNumberId.'/messages';

        try {
            $response = Http::withToken($token)
                ->timeout($this->config['timeout'] ?? 15)
                ->asJson()
                ->post($url, $this->payload($message));
        } catch (Throwable $e) {
            $this->fail($message, 'Request failed: '.$e->getMessage());

            return;
        }

        if ($response->successful()) {
            $message->forceFill([
                'driver' => $this->name(),
                'status' => Message::STATUS_SENT,
                'sent_at' => now(),
                'provider_message_id' => data_get($response->json(), 'messages.0.id'),
                'error' => null,
            ])->save();

            return;
        }

        $this->fail($message, $this->errorFrom($response->json(), $response->status()));
    }

    private function payload(Message $message): array
    {
        $to = Phone::digits($message->to_phone);
        $template = $this->config['templates'][$message->template_key] ?? null;

        if ($template) {
            return [
                'messaging_product' => 'whatsapp',
                'to' => $to,
                'type' => 'template',
                'template' => [
                    'name' => $template,
                    'language' => ['code' => $this->languageCode($message->locale)],
                    'components' => [[
                        'type' => 'body',
                        'parameters' => [['type' => 'text', 'text' => $message->body]],
                    ]],
                ],
            ];
        }

        return [
            'messaging_product' => 'whatsapp',
            'to' => $to,
            'type' => 'text',
            'text' => ['preview_url' => true, 'body' => $message->body],
        ];
    }

    /** Meta wants sq/en style codes; some templates are registered as en_US. */
    private function languageCode(string $locale): string
    {
        return $this->config['language_map'][$locale] ?? $locale;
    }

    private function errorFrom(?array $json, int $status): string
    {
        $message = data_get($json, 'error.message');
        $code = data_get($json, 'error.code');

        if ($message) {
            return trim('HTTP '.$status.' '.($code ? "($code) " : '').$message);
        }

        return 'HTTP '.$status;
    }

    private function fail(Message $message, string $error): void
    {
        $message->forceFill([
            'driver' => $this->name(),
            'status' => Message::STATUS_FAILED,
            'error' => $error,
        ])->save();
    }

    public function name(): string
    {
        return 'cloud_api';
    }

    public function requiresInstallerAction(): bool
    {
        return false;
    }
}
