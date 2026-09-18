<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // What the customer sends back when they tap "book my service" on the card.
        Schema::create('booking_requests', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('installation_id')->constrained()->cascadeOnDelete();
            $table->string('preferred_window', 40)->nullable(); // this_week | next_week | morning | afternoon
            $table->text('note')->nullable();
            $table->string('contact_phone', 32)->nullable();
            // new | scheduled | done | declined
            $table->string('status', 20)->default('new');
            $table->date('scheduled_for')->nullable();
            $table->timestamp('resolved_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'status']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('booking_requests');
    }
};
