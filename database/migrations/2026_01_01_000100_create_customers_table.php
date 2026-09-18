<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('customers', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->string('name')->nullable();
            $table->string('phone', 32);              // E.164 where possible; this is the identity of a customer
            $table->string('address')->nullable();
            $table->string('city')->nullable();
            $table->decimal('lat', 10, 7)->nullable();
            $table->decimal('lng', 10, 7)->nullable();
            $table->string('locale', 5)->default('sq'); // the language the customer is written to in
            $table->text('notes')->nullable();
            // Real opt-in: set when the customer agrees to be messaged. Nothing is sent without it.
            // The exact wording shown is stored, not a reference to it, because the wording
            // may change later and what matters is what this person actually agreed to.
            $table->timestamp('messaging_consent_at')->nullable();
            $table->text('consent_text')->nullable();
            $table->string('consent_method', 40)->nullable(); // in_person_at_install | web_form | imported
            $table->timestamp('opted_out_at')->nullable();
            $table->timestamps();

            $table->unique(['user_id', 'phone']);
            $table->index(['user_id', 'name']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('customers');
    }
};
