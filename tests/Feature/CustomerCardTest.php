<?php

namespace Tests\Feature;

use App\Models\BookingRequest;
use App\Models\Installation;
use App\Models\Message;
use App\Models\Reminder;
use App\Models\User;
use App\Services\InstallationRegistrar;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CustomerCardTest extends TestCase
{
    use RefreshDatabase;

    private function install(string $customerLocale = 'sq'): Installation
    {
        $user = User::create([
            'name' => 'Arben Hoxha',
            'business_name' => 'Hoxha Termoteknikë',
            'email' => 'arben@example.com',
            'phone' => '+355691110000',
            'locale' => 'sq',
            'timezone' => 'Europe/Tirane',
            'password' => 'password',
        ]);

        return app(InstallationRegistrar::class)->register($user, [
            'phone' => '0692001122',
            'customer_name' => 'Miranda Leka',
            'customer_locale' => $customerLocale,
            'appliance_type' => 'gas_boiler',
            'brand' => 'Vaillant',
            'serial' => 'VA24-118837',
            'messaging_consent' => true,
        ]);
    }

    public function test_the_card_opens_with_no_login_and_shows_the_proof(): void
    {
        $install = $this->install();

        $this->get(route('card.show', $install->public_token))
            ->assertOk()
            ->assertSee('VA24-118837')
            ->assertSee('Hoxha Termoteknikë')
            ->assertSee($install->installed_on->format('d/m/Y'));
    }

    public function test_the_card_is_written_in_the_customers_language_not_the_installers(): void
    {
        $albanian = $this->get(route('card.show', $this->install('sq')->public_token))->getContent();
        $this->assertStringContainsString(__('card.statutory', [], 'sq'), $albanian);

        Installation::query()->delete();
        \App\Models\Customer::query()->delete();
        User::query()->delete();

        $english = $this->get(route('card.show', $this->install('en')->public_token))->getContent();
        $this->assertStringContainsString(__('card.statutory', [], 'en'), $english);
    }

    /**
     * The two guarantees are shown separately. Merging them is the small dishonesty that
     * lets a reminder imply a missed service costs the customer their rights.
     */
    public function test_the_card_separates_the_statutory_guarantee_from_the_manufacturer_warranty(): void
    {
        $install = $this->install('en');

        $response = $this->get(route('card.show', $install->public_token))->assertOk();

        $response->assertSee(__('card.statutory', [], 'en'));
        $response->assertSee(__('card.manufacturer', [], 'en'));
        $response->assertSee('It cannot be lost by missing a service', false);
        $this->assertSame(
            $install->installed_on->copy()->addMonths(24)->toDateString(),
            $install->statutoryGuaranteeUntil()->toDateString()
        );
    }

    public function test_opening_the_card_is_recorded_for_the_installer(): void
    {
        $install = $this->install();
        $this->assertSame(0, $install->card_view_count);

        $this->get(route('card.show', $install->public_token));
        $this->get(route('card.show', $install->public_token));

        $install->refresh();
        $this->assertSame(2, $install->card_view_count);
        $this->assertNotNull($install->card_first_viewed_at);
    }

    public function test_asking_for_a_service_reaches_the_installer_and_stops_the_nagging(): void
    {
        $install = $this->install();

        $this->post(route('card.book.store', $install->public_token), [
            'preferred_window' => 'this_week',
            'note' => 'Mornings are easier.',
        ])->assertRedirect(route('card.show', $install->public_token));

        $booking = BookingRequest::firstOrFail();
        $this->assertSame(BookingRequest::STATUS_NEW, $booking->status);
        $this->assertSame('this_week', $booking->preferred_window);
        $this->assertSame($install->user_id, $booking->user_id);

        $this->assertSame(
            0,
            $install->reminders()->where('status', Reminder::STATUS_PENDING)->where('kind', Reminder::KIND_SERVICE_DUE)->count(),
            'a customer who has asked should not then be reminded to ask'
        );

        $this->assertTrue(Message::where('template_key', Message::TEMPLATE_BOOKING_ACK)->exists());
    }

    public function test_asking_twice_does_not_create_a_queue(): void
    {
        $install = $this->install();

        $this->post(route('card.book.store', $install->public_token), ['preferred_window' => 'this_week']);
        $this->post(route('card.book.store', $install->public_token), ['preferred_window' => 'next_week']);

        $this->assertSame(1, BookingRequest::count());
        $this->assertSame('next_week', BookingRequest::first()->preferred_window);
    }

    public function test_one_tap_stops_every_future_message(): void
    {
        $install = $this->install();

        $this->post(route('card.stop.store', $install->public_token))
            ->assertRedirect(route('card.show', $install->public_token));

        $install->refresh();
        $this->assertNotNull($install->customer->opted_out_at);
        $this->assertFalse($install->customer->canBeMessaged());
        $this->assertSame(0, $install->reminders()->where('status', Reminder::STATUS_PENDING)->count());

        // And nothing can be queued for them afterwards.
        Message::query()->delete();
        app(\App\Services\Messaging\MessageDispatcher::class)->sendCard($install->fresh(['customer', 'user']));
        $this->assertSame(Message::STATUS_SKIPPED, Message::firstOrFail()->status);
    }

    public function test_the_card_is_not_indexed_by_search_engines(): void
    {
        $install = $this->install();

        $this->get(route('card.show', $install->public_token))
            ->assertOk()
            ->assertSee('noindex', false);
    }

    public function test_the_printable_card_carries_the_same_facts(): void
    {
        $install = $this->install();

        $this->get(route('card.print', $install->public_token))
            ->assertOk()
            ->assertSee('VA24-118837')
            ->assertSee($install->public_token);
    }
}
