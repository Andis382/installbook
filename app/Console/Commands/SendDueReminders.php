<?php

namespace App\Console\Commands;

use App\Models\Installation;
use App\Models\Reminder;
use App\Services\Messaging\MessageDispatcher;
use App\Services\ReminderPlanner;
use Illuminate\Console\Command;
use Illuminate\Support\Carbon;

/**
 * The engine. Run once a day from cron; it is safe to run more often and safe to run
 * twice, because a reminder is claimed by its status before anything is composed.
 */
class SendDueReminders extends Command
{
    protected $signature = 'installbook:send-reminders
                            {--date= : Pretend today is this date (Y-m-d), for testing}
                            {--dry-run : Show what would happen and change nothing}
                            {--limit=500 : Maximum reminders to process in one run}';

    protected $description = 'Fire every reminder that has come due: service reminders and warranty notices.';

    public function handle(MessageDispatcher $dispatcher, ReminderPlanner $planner): int
    {
        $today = $this->option('date') ? Carbon::parse($this->option('date'))->startOfDay() : Carbon::today();
        $dry = (bool) $this->option('dry-run');

        $due = Reminder::query()
            ->with(['installation.customer', 'installation.user'])
            ->where('status', Reminder::STATUS_PENDING)
            ->whereDate('fire_on', '<=', $today)
            ->orderBy('fire_on')
            ->limit((int) $this->option('limit'))
            ->get();

        if ($due->isEmpty()) {
            $this->info('Nothing due on '.$today->toDateString().'.');

            return self::SUCCESS;
        }

        $counts = ['sent' => 0, 'skipped' => 0, 'stale' => 0];

        foreach ($due as $reminder) {
            $installation = $reminder->installation;

            if (! $installation || $installation->status !== Installation::STATUS_ACTIVE) {
                $this->resolve($reminder, Reminder::STATUS_SKIPPED, 'installation_inactive', $dry);
                $counts['skipped']++;

                continue;
            }

            // A backlog after downtime should not become a burst of late messages.
            if ($planner->isStale($reminder, $today)) {
                $this->resolve($reminder, Reminder::STATUS_SKIPPED, 'stale', $dry);
                $counts['stale']++;

                continue;
            }

            if (! $installation->customer?->canBeMessaged()) {
                $this->resolve($reminder, Reminder::STATUS_SKIPPED, 'no_consent', $dry);
                $counts['skipped']++;

                continue;
            }

            $line = sprintf(
                '%s  %s  %s  → %s',
                $reminder->fire_on->toDateString(),
                str_pad($reminder->kind, 16),
                str_pad(mb_substr($installation->unitName(), 0, 28), 28),
                $installation->customer->phone,
            );

            if ($dry) {
                $this->line('would send: '.$line);
                $counts['sent']++;

                continue;
            }

            $message = match ($reminder->kind) {
                Reminder::KIND_SERVICE_DUE => $dispatcher->sendServiceDue(
                    $installation,
                    dueToday: $installation->next_service_due_on?->lte($today) ?? false,
                ),
                Reminder::KIND_WARRANTY_ENDING => $dispatcher->sendWarrantyEnding($installation),
                default => null,
            };

            $reminder->forceFill([
                'status' => Reminder::STATUS_FIRED,
                'fired_at' => now(),
                'message_id' => $message?->id,
            ])->save();

            $this->line('queued: '.$line);
            $counts['sent']++;
        }

        $this->newLine();
        $this->info(sprintf(
            '%s: %d queued, %d skipped, %d stale.%s',
            $dry ? 'Dry run' : 'Done',
            $counts['sent'],
            $counts['skipped'],
            $counts['stale'],
            $dispatcher->requiresInstallerAction() ? ' Open the outbox to send them.' : '',
        ));

        return self::SUCCESS;
    }

    private function resolve(Reminder $reminder, string $status, string $reason, bool $dry): void
    {
        if ($dry) {
            $this->line("would skip ({$reason}): reminder #{$reminder->id}");

            return;
        }

        $reminder->forceFill(['status' => $status, 'reason' => $reason])->save();
    }
}
