<?php

namespace App\Providers;

use App\Services\Messaging\Contracts\WhatsAppDriver;
use App\Services\Messaging\Drivers\CloudApiDriver;
use App\Services\Messaging\Drivers\LogDriver;
use App\Services\Messaging\Drivers\ManualDriver;
use App\Services\Plate\AnthropicPlateReader;
use App\Services\Plate\NullPlateReader;
use App\Services\Plate\PlateReader;
use Illuminate\Support\ServiceProvider;

class InstallBookServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(WhatsAppDriver::class, function ($app) {
            $config = $app['config']['installbook.messaging'];

            return match ($config['driver'] ?? 'manual') {
                'log' => new LogDriver,
                'cloud_api' => new CloudApiDriver($config['cloud_api']),
                default => new ManualDriver,
            };
        });

        $this->app->singleton(PlateReader::class, function ($app) {
            $config = $app['config']['installbook.ocr'];

            if (($config['driver'] ?? 'none') !== 'anthropic') {
                return new NullPlateReader;
            }

            return new AnthropicPlateReader(
                apiKey: $config['api_key'] ?? null,
                model: $config['model'],
                maxTokens: $config['max_tokens'],
                timeout: $config['timeout'],
            );
        });
    }
}
