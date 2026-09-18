<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // A split air conditioner has two data plates, a multi-split has several, a solar
        // array has one per panel plus the inverter. The installation carries the warranty
        // and the service date; each physical box that has its own serial gets a row here.
        //
        // The capture flow only ever asks for one plate, which lands on the installation
        // itself. Extra plates are added afterwards, from the van, when there is time.
        Schema::create('installation_plates', function (Blueprint $table) {
            $table->id();
            $table->foreignId('installation_id')->constrained()->cascadeOnDelete();
            $table->string('role', 30);          // indoor | outdoor | inverter | panel | main | other
            $table->string('brand')->nullable();
            $table->string('model')->nullable();
            $table->string('serial')->nullable();
            $table->string('photo_path')->nullable();
            $table->string('note')->nullable();
            $table->timestamps();

            $table->index('installation_id');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('installation_plates');
    }
};
