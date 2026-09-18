<?php

namespace Tests\Feature;

use App\Models\Installation;
use App\Models\Message;
use App\Models\Reminder;
use App\Models\User;
use App\Services\InstallationRegistrar;
use App\Services\ReminderPlanner;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Support\Carbon;
use Tests\TestCase;

class ReminderEngineTest extends TestCase
{
    use RefreshDatabase;

    private function install(array $overrides = []): Installation
    {
        $user = User::create([
            'name' => 'Arben',
            'email' => 'arben@example.com',
            'locale' => 'sq',
            'timezone' => 'Europe/Tirane',
            'password' => 'password',
            'reminder_lead_days' => 30,
        ]);

        return app(InstallationRegistrar::class)->register($user, array_merge([
            'phone' => '0692001122',
            'customer_name' => 'Miranda',
            'appliance_type' => 'water_heater', // no seasonal shift, so dates are predictable
            'messaging_consent' => true,
        ], $overrides));
    }

    public function test_an_install_plans_a_warning_a_due_day_and_a_warranty_notice(): void
    {
        $install = $this->install(['installed_on' => now()->subMonths(6)->toDateString()]);

        $kinds = $install->reminders()->where('status', Reminder::STATUS_PENDING)->pluck('kind', 'fire_on');

        $this->assertGreaterThanOrEqual(2, $kinds->count());
        $this->assertTrue($install->reminders()->where('kind', Reminder::KIND_SERVICE_DUE)->exists());

        $lead = $install->reminders()
            ->where('kind', Reminder::KIND_SERVICE_DUE)
            ->orderBy('fire_on')
            ->first();

        $this->assertSame(
            $install->next_service_due_on->copy()->subDays(30)->toDateString(),
            $lead->fire_on->toDateString(),
            'the first warning lands the installer-configured number of days before the date'
        );
    }

    public function test_reminders_are_never_planned_in_the_past(): void
    {
        $install = $this->install(['installed_on' => now()->subMonths(30)->toDateString()]);

        $pending = $install->reminders()->where('status', Reminder::STATUS_PENDING)->get();

        $this->assertNotEmpty($pending);
        foreach ($pending as $reminder) {
            $this->assertTrue($reminder->fire_on->greaterThanOrEqualTo(Carbon::today()), 'planned in the past');
        }
    }

    /**
     * A unit recorded long after it was fitted is already overdue. Every ordinary slot for it
     * is in the past, so without a catch-up the customer would simply never be told.
     */
    public function test_an_already_overdue_unit_still_gets_one_message(): void
    {
        $install = $this->install(['installed_on' => now()->subMonths(30)->toDateString()]);

        $this->assertTrue($install->isOverdue(), 'the fixture should be overdue');

        $catchUp = $install->reminders()
            ->where('kind', Reminder::KIND_SERVICE_DUE)
            ->where('status', Reminder::STATUS_PENDING)
            ->get();

        $this->assertCount(1, $catchUp, 'exactly one catch-up, not one per missed slot');
        $this->assertSame(Carbon::today()->toDateString(), $catchUp->first()->fire_on->toDateString());

        Message::query()->delete();
        $this->artisan('installbook:send-reminders')->assertSuccessful();
        $this->assertSame(1, Message::where('template_key', Message::TEMPLATE_SERVICE_DUE)->count());

        // And editing the unit afterwards must not queue a second one.
        app(\App\Services\InstallationRegistrar::class)->refreshSchedule($install->refresh());
        $this->assertSame(
            0,
            $install->reminders()->where('kind', Reminder::KIND_SERVICE_DUE)->where('status', Reminder::STATUS_PENDING)->count(),
            'the catch-up should not be re-planned once it has gone out'
        );
    }

    public function test_the_command_queues_a_message_and_marks_the_reminder_fired(): void
    {
        $install = $this->install();
        $reminder = $install->reminders()->where('kind', Reminder::KIND_SERVICE_DUE)->orderBy('fire_on')->firstOrFail();

        Message::query()->delete(); // ignore the card queued at install time

        $this->artisan('installbook:send-reminders', ['--date' => $reminder->fire_on->toDateString()])
            ->assertSuccessful();

        $reminder->refresh();
        $this->assertSame(Reminder::STATUS_FIRED, $reminder->status);
        $this->assertNotNull($reminder->message_id);

        $message = Message::findOrFail($reminder->message_id);
        $this->assertSame(Message::TEMPLATE_SERVICE_DUE, $message->template_key);
        $this->assertSame('sq', $message->locale, 'the customer is written to in their own language');
        $this->assertStringContainsString($install->public_token, $message->body);
    }

    public function test_a_dry_run_changes_nothing(): void
    {
        $install = $this->install();
        $reminder = $install->reminders()->orderBy('fire_on')->firstOrFail();
        $before = Message::count();

        $this->artisan('installbook:send-reminders', ['--date' => $reminder->fire_on->toDateString(), '--dry-run' => true])
            ->assertSuccessful();

        $this->assertSame($before, Message::count());
        $this->assertSame(Reminder::STATUS_PENDING, $reminder->refresh()->status);
    }

    /**
     * A week of downtime must not turn into a week of late messages arriving at once.
     */
    public function test_a_long_overdue_reminder_is_skipped_rather_than_sent_late(): void
    {
        $install = $this->install();
        $reminder = $install->reminders()->orderBy('fire_on')->firstOrFail();

        $stale = $reminder->fire_on->copy()->addDays(ReminderPlanner::STALE_AFTER_DAYS + 1);
        Message::query()->delete();

        $this->artisan('installbook:send-reminders', ['--date' => $stale->toDateString()])->assertSuccessful();

        $reminder->refresh();
        $this->assertSame(Reminder::STATUS_SKIPPED, $reminder->status);
        $this->assertSame('stale', $reminder->reason);
        $this->assertSame(0, Message::count());
    }

    public function test_a_customer_who_opted_out_is_not_messaged(): void
    {
        $install = $this->install();
        $install->customer->update(['opted_out_at' => now()]);
        $reminder = $install->reminders()->orderBy('fire_on')->firstOrFail();
        Message::query()->delete();

        $this->artisan('installbook:send-reminders', ['--date' => $reminder->fire_on->toDateString()])->assertSuccessful();

        $this->assertSame(Reminder::STATUS_SKIPPED, $reminder->refresh()->status);
        $this->assertSame('no_consent', $reminder->reason);
        $this->assertSame(0, Message::count());
    }

    public function test_running_twice_does_not_send_twice(): void
    {
        $install = $this->install();
        $reminder = $install->reminders()->orderBy('fire_on')->firstOrFail();
        Message::query()->delete();

        $this->artisan('installbook:send-reminders', ['--date' => $reminder->fire_on->toDateString()])->assertSuccessful();
        $this->artisan('installbook:send-reminders', ['--date' => $reminder->fire_on->toDateString()])->assertSuccessful();

        $this->assertSame(1, Message::where('template_key', Message::TEMPLATE_SERVICE_DUE)->count());
    }

    public function test_the_warranty_sentence_only_appears_when_the_unit_truly_requires_a_service(): void
    {
        $plain = $this->install(['service_required_for_warranty' => false]);
        $composer = app(\App\Services\Messaging\MessageComposer::class);

        $this->assertStringNotContainsStringIgnoringCase('garanc', $composer->serviceDue($plain, false));

        $plain->update(['service_required_for_warranty' => true]);
        $this->assertStringContainsStringIgnoringCase('garanc', $composer->serviceDue($plain->refresh(), false));
    }
}
