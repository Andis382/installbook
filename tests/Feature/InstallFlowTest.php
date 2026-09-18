<?php

namespace Tests\Feature;

use App\Models\Installation;
use App\Models\Message;
use App\Models\Reminder;
use App\Models\User;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InstallFlowTest extends TestCase
{
    use RefreshDatabase;

    private function installer(array $attributes = []): User
    {
        return User::create(array_merge([
            'name' => 'Arben',
            'business_name' => 'Hoxha Termo',
            'email' => 'arben@example.com',
            'phone' => '+355691110000',
            'locale' => 'sq',
            'timezone' => 'Europe/Tirane',
            'password' => 'password',
        ], $attributes));
    }

    /**
     * The rule the whole product depends on: the form never refuses. A machine recorded
     * with nothing but its type is still a record, and the installer can finish it later.
     */
    public function test_an_install_saves_with_nothing_but_a_type(): void
    {
        $user = $this->installer();

        $response = $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'gas_boiler',
        ]);

        $install = Installation::first();

        $this->assertNotNull($install, 'the install was refused');
        $response->assertRedirect(route('installations.show', $install));
        $this->assertNull($install->customer_id);
        $this->assertNull($install->serial);
        $this->assertNotNull($install->public_token);
        $this->assertSame(24, $install->warranty_months, 'defaults come from the appliance type');
        $this->assertSame(12, $install->service_interval_months);
        $this->assertNotNull($install->next_service_due_on);
        $this->assertNotNull($install->registration_deadline_on);
    }

    public function test_a_full_install_creates_the_customer_the_first_visit_and_the_card(): void
    {
        $user = $this->installer();

        $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'gas_boiler',
            'phone' => '069 200 1122',
            'customer_name' => 'Miranda',
            'address' => 'Rruga e Kavajës 12',
            'brand' => 'Vaillant',
            'model' => 'ecoTEC plus',
            'serial' => 'VA24-118837',
            'messaging_consent' => '1',
        ])->assertRedirect();

        $install = Installation::firstOrFail();

        $this->assertSame('+355692001122', $install->customer->phone, 'the phone number is normalised on the way in');
        $this->assertNotNull($install->customer->messaging_consent_at);
        $this->assertNotEmpty($install->customer->consent_text, 'the wording agreed to is stored, not just a flag');
        $this->assertSame('in_person_at_install', $install->customer->consent_method);
        $this->assertSame(1, $install->visits()->where('kind', 'install')->count());

        $card = Message::where('template_key', Message::TEMPLATE_CARD)->firstOrFail();
        $this->assertSame(Message::STATUS_QUEUED, $card->status, 'the manual driver leaves it for the installer to send');
        $this->assertStringContainsString($install->public_token, $card->body);
        $this->assertStringContainsString('VA24-118837', $card->body);
    }

    public function test_the_whatsapp_link_is_encoded_the_way_whatsapp_needs(): void
    {
        $user = $this->installer();
        $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'split_ac',
            'phone' => '069 200 1122',
            'messaging_consent' => '1',
        ]);

        $link = Message::firstOrFail()->whatsappLink();

        $this->assertStringStartsWith('https://wa.me/355692001122?text=', $link);
        $this->assertStringNotContainsString('+', substr($link, strpos($link, '?text=')), 'spaces must be %20, never +');
        $this->assertStringContainsString('%0A', $link, 'newlines must survive as %0A');
    }

    public function test_a_message_with_no_number_falls_back_to_the_contact_picker(): void
    {
        $message = new Message(['to_phone' => '', 'body' => 'hello there']);

        $this->assertSame('https://wa.me/?text=hello%20there', $message->whatsappLink());
    }

    public function test_nothing_is_queued_for_a_customer_who_never_agreed(): void
    {
        $user = $this->installer();

        $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'gas_boiler',
            'phone' => '069 200 1122',
            // no consent given
        ]);

        $message = Message::firstOrFail();
        $this->assertSame(Message::STATUS_SKIPPED, $message->status);
        $this->assertSame('no_messaging_consent', $message->error);
    }

    public function test_recording_a_service_restarts_the_clock_and_replans_the_reminders(): void
    {
        $user = $this->installer();
        $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'water_heater',
            'phone' => '069 200 1122',
            'messaging_consent' => '1',
            'installed_on' => now()->subMonths(24)->toDateString(),
        ]);

        $install = Installation::firstOrFail();
        $originalDue = $install->next_service_due_on->toDateString();

        $this->actingAs($user)->post(route('visits.store', $install), [
            'kind' => 'service',
            'performed_on' => now()->toDateString(),
            'price' => '45',
        ])->assertRedirect();

        $install->refresh();
        $this->assertNotSame($originalDue, $install->next_service_due_on->toDateString());
        $this->assertTrue($install->next_service_due_on->isFuture());
        $this->assertSame(2, $install->visits()->count());
        $this->assertSame(4500, $install->visits()->where('kind', 'service')->first()->price_cents);

        // Reminders for the previous cycle must not survive into the new one.
        $pending = $install->reminders()->where('status', Reminder::STATUS_PENDING)->get();
        foreach ($pending as $reminder) {
            $this->assertTrue($reminder->fire_on->greaterThanOrEqualTo(now()->startOfDay()));
        }
    }

    public function test_a_serial_is_found_by_a_search_that_confuses_characters(): void
    {
        $user = $this->installer();
        $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'gas_boiler',
            'serial' => 'O1S8-B2G',
        ]);

        // Punctuation is dropped and confusable characters folded; the typed serial is kept.
        $this->assertSame('O1S8-B2G', Installation::first()->serial);
        $this->assertSame('0158826', Installation::first()->serial_key);

        $this->actingAs($user)
            ->get(route('installations.index', ['q' => '0158826']))
            ->assertOk()
            ->assertSee('O1S8-B2G');
    }

    public function test_archiving_stops_the_reminders_but_keeps_the_record(): void
    {
        $user = $this->installer();
        $this->actingAs($user)->post(route('installations.store'), [
            'appliance_type' => 'gas_boiler',
            'phone' => '069 200 1122',
            'messaging_consent' => '1',
        ]);
        $install = Installation::firstOrFail();

        $this->actingAs($user)->post(route('installations.archive', $install))->assertRedirect();

        $install->refresh();
        $this->assertSame(Installation::STATUS_ARCHIVED, $install->status);
        $this->assertSame(0, $install->reminders()->where('status', Reminder::STATUS_PENDING)->count());
        $this->assertNotNull($install->public_token, 'the customer keeps their card');
    }
}
