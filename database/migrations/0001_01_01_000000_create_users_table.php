<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // The installer. One account is one business; there are no seats and no office user.
        Schema::create('users', function (Blueprint $table) {
            $table->id();
            $table->string('name');
            $table->string('business_name')->nullable();
            $table->string('email')->unique();
            $table->string('phone')->nullable();          // shown to customers on the card
            $table->string('city')->nullable();
            $table->string('locale', 5)->default('sq');   // the installer's own UI language
            $table->string('timezone', 64)->default('Europe/Tirane');
            $table->string('logo_path')->nullable();
            $table->text('card_footer')->nullable();      // free text printed on every warranty card
            $table->string('registration_number')->nullable(); // installer registration number, where a market has one
            $table->unsignedSmallInteger('reminder_lead_days')->default(30);  // how early the customer is told
            $table->unsignedSmallInteger('registration_window_days')->default(30); // manufacturer registration deadline
            $table->timestamp('email_verified_at')->nullable();
            $table->string('password');
            $table->rememberToken();
            $table->timestamps();
        });

        Schema::create('password_reset_tokens', function (Blueprint $table) {
            $table->string('email')->primary();
            $table->string('token');
            $table->timestamp('created_at')->nullable();
        });

        Schema::create('sessions', function (Blueprint $table) {
            $table->string('id')->primary();
            $table->foreignId('user_id')->nullable()->index();
            $table->string('ip_address', 45)->nullable();
            $table->text('user_agent')->nullable();
            $table->longText('payload');
            $table->integer('last_activity')->index();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('users');
        Schema::dropIfExists('password_reset_tokens');
        Schema::dropIfExists('sessions');
    }
};
