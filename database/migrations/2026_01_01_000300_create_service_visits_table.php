<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Every time someone touched this machine. The install itself is the first visit.
        Schema::create('service_visits', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            $table->foreignId('installation_id')->constrained()->cascadeOnDelete();
            $table->string('kind', 20);           // install | service | repair | inspection
            $table->date('performed_on');
            $table->text('notes')->nullable();
            $table->string('photo_path')->nullable();
            $table->unsignedInteger('price_cents')->nullable();
            $table->string('currency', 3)->default('EUR');
            $table->timestamps();

            $table->index(['installation_id', 'performed_on']);
            $table->index(['user_id', 'performed_on']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('service_visits');
    }
};
