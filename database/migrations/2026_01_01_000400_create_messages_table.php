<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // The outbox. Every message the app wants sent is written here first, in the
        // customer's language, and is visible to the installer before it goes anywhere.
        Schema::create('messages', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('customer_id')->nullable()->constrained()->nullOnDelete();
            $table->foreignId('installation_id')->nullable()->constrained()->nullOnDelete();

            $table->string('template_key', 40);   // card | service_due | warranty_ending | booking_ack
            $table->string('locale', 5);
            $table->string('to_phone', 32);
            $table->text('body');
            $table->string('link')->nullable();   // the card link included in the body

            $table->string('channel', 20)->default('whatsapp');
            $table->string('driver', 20);         // manual | log | cloud_api
            // queued  : waiting to be sent or handed to the installer
            // sent    : confirmed away (API accepted it, or the installer marked it sent)
            // failed  : the driver refused it
            // skipped : deliberately not sent (no consent, opted out, duplicate)
            $table->string('status', 20)->default('queued');
            $table->string('provider_message_id')->nullable();
            $table->text('error')->nullable();
            $table->timestamp('sent_at')->nullable();
            $table->timestamps();

            $table->index(['user_id', 'status']);
            $table->index(['installation_id', 'template_key']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('messages');
    }
};
