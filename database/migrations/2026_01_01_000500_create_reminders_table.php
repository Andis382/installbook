<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // One row per thing that should be said about one installation at one time.
        // Created up front so the installer can see what is coming and cancel it.
        Schema::create('reminders', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('installation_id')->constrained()->cascadeOnDelete();
            $table->string('kind', 30);          // service_due | warranty_ending
            $table->string('audience', 20);      // customer | installer
            $table->date('fire_on');
            // pending | fired | cancelled | skipped
            $table->string('status', 20)->default('pending');
            $table->foreignId('message_id')->nullable()->constrained('messages')->nullOnDelete();
            $table->timestamp('fired_at')->nullable();
            $table->string('reason')->nullable(); // why it was cancelled or skipped
            $table->timestamps();

            $table->index(['status', 'fire_on']);
            $table->index(['user_id', 'fire_on']);
            $table->unique(['installation_id', 'kind', 'audience', 'fire_on'], 'reminders_unique_slot');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('reminders');
    }
};
