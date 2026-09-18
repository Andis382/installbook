<?php

use Illuminate\Support\Facades\Schedule;

// One run a day is enough: every reminder is a date, not a time.
// Add this line to cron so the scheduler itself runs:
//   * * * * * cd /path/to/installbook && php artisan schedule:run >> /dev/null 2>&1
Schedule::command('installbook:send-reminders')
    ->dailyAt('08:00')
    ->withoutOverlapping()
    ->onOneServer();
