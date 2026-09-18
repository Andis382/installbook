<?php

namespace Tests\Feature;

use App\Models\Installation;
use App\Models\User;
use App\Services\InstallationRegistrar;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

/**
 * One installer must never see another's book. The failure mode is answered with 404 and
 * not 403, so a probe cannot even learn that an id exists.
 */
class TenancyTest extends TestCase
{
    use RefreshDatabase;

    private function installerWithInstall(string $email): array
    {
        $user = User::create([
            'name' => 'Installer '.$email,
            'email' => $email,
            'locale' => 'sq',
            'timezone' => 'Europe/Tirane',
            'password' => 'password',
        ]);

        $install = app(InstallationRegistrar::class)->register($user, [
            'phone' => '069'.random_int(1000000, 9999999),
            'customer_name' => 'Customer of '.$email,
            'appliance_type' => 'gas_boiler',
            'serial' => strtoupper(substr(md5($email), 0, 10)),
            'messaging_consent' => true,
        ]);

        return [$user, $install];
    }

    public function test_another_installers_records_are_not_found(): void
    {
        [$alice, $aliceInstall] = $this->installerWithInstall('alice@example.com');
        [$bob, $bobInstall] = $this->installerWithInstall('bob@example.com');

        $routes = [
            ['get', route('installations.show', $bobInstall)],
            ['get', route('installations.edit', $bobInstall)],
            ['post', route('installations.archive', $bobInstall)],
            ['post', route('installations.registered', $bobInstall)],
            ['post', route('installations.card', $bobInstall)],
            ['get', route('customers.show', $bobInstall->customer)],
            ['get', route('customers.export', $bobInstall->customer)],
        ];

        foreach ($routes as [$method, $url]) {
            $this->actingAs($alice)->{$method}($url)->assertNotFound("{$method} {$url} leaked");
        }

        $this->assertNotSame($aliceInstall->id, $bobInstall->id);
    }

    public function test_listings_only_ever_contain_your_own_rows(): void
    {
        [$alice] = $this->installerWithInstall('alice@example.com');
        [$bob, $bobInstall] = $this->installerWithInstall('bob@example.com');

        $this->actingAs($alice)->get(route('installations.index'))
            ->assertOk()
            ->assertDontSee($bobInstall->serial);

        $this->actingAs($alice)->get(route('customers.index'))
            ->assertOk()
            ->assertDontSee('Customer of bob@example.com');

        $this->assertSame(1, $alice->installations()->count());
        $this->assertSame(1, $alice->customers()->count());
    }

    public function test_writing_to_another_installers_unit_is_not_possible(): void
    {
        [$alice] = $this->installerWithInstall('alice@example.com');
        [$bob, $bobInstall] = $this->installerWithInstall('bob@example.com');

        $this->actingAs($alice)->post(route('visits.store', $bobInstall), [
            'kind' => 'service',
            'performed_on' => now()->toDateString(),
        ])->assertNotFound();

        $this->assertSame(1, $bobInstall->visits()->count(), 'only the original install visit');
    }

    public function test_guests_are_sent_to_the_login_page(): void
    {
        foreach ([route('dashboard'), route('installations.index'), route('outbox.index'), route('settings.edit')] as $url) {
            $this->get($url)->assertRedirect(route('login'));
        }
    }

    public function test_the_public_card_needs_the_token_and_nothing_else(): void
    {
        [, $install] = $this->installerWithInstall('alice@example.com');

        $this->get(route('card.show', $install->public_token))->assertOk();
        $this->get('/c/'.str_repeat('a', 24))->assertNotFound();
    }

    public function test_the_card_token_is_long_enough_to_not_be_guessed(): void
    {
        $token = Installation::newToken();

        $this->assertGreaterThanOrEqual(20, strlen($token));
        $this->assertNotSame($token, Installation::newToken());
    }
}
