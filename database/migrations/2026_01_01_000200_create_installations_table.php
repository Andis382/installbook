<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // The unit of work: a machine, with a serial, at an address. Not a job, not a customer.
        Schema::create('installations', function (Blueprint $table) {
            $table->id();
            $table->foreignId('user_id')->constrained()->cascadeOnDelete();
            // Nullable on purpose. A machine recorded with nothing but a photo is still a
            // useful record, and a form that refuses to save is a form that stops being used.
            $table->foreignId('customer_id')->nullable()->constrained()->nullOnDelete();

            $table->string('appliance_type', 40);
            $table->string('brand')->nullable();
            $table->string('model')->nullable();
            $table->string('serial')->nullable();
            // The serial as typed is what gets printed and quoted. This second copy folds the
            // characters people and cameras confuse (O/0, I/1, S/5, B/8) so that searching for
            // what you think you saw still finds what was actually recorded. Search only.
            $table->string('serial_key')->nullable()->index();
            $table->string('extra_code')->nullable();      // GC number, PNC, E-number, whatever the plate carries
            $table->string('capacity')->nullable();        // "24 kW", "12000 BTU"
            $table->string('refrigerant')->nullable();     // on the plate of anything that cools
            $table->string('plate_photo_path')->nullable();
            $table->string('unit_photo_path')->nullable();
            $table->json('plate_reading')->nullable();     // raw OCR output, kept so a misread can be audited

            $table->date('installed_on');
            // The manufacturer's own warranty. Separate from the statutory guarantee a buyer
            // has by law, which is unconditional and is computed rather than stored.
            $table->unsignedSmallInteger('warranty_months');
            $table->date('warranty_expires_on')->nullable();
            // Most manufacturer warranties only start if the unit is registered within a
            // window after installation. Missing that costs the customer years of cover and
            // is the first thing this register is worth, long before the first service.
            $table->date('registration_deadline_on')->nullable();
            $table->timestamp('manufacturer_registered_at')->nullable();
            $table->unsignedSmallInteger('service_interval_months');
            $table->date('next_service_due_on')->nullable();
            // True only when the manufacturer genuinely requires a logged annual service.
            // It controls whether the reminder may mention the warranty at all.
            $table->boolean('service_required_for_warranty')->default(false);

            $table->string('location_note')->nullable();   // "kitchen", "roof, north side", "flat 4B"
            $table->text('notes')->nullable();
            $table->string('public_token', 32)->unique();  // unguessable customer card link
            // Whether the customer ever opened their card. The installer sees it, and it is
            // the single most convincing thing to show a sceptical installer in a demo.
            $table->unsignedInteger('card_view_count')->default(0);
            $table->timestamp('card_first_viewed_at')->nullable();
            $table->string('status', 20)->default('active'); // active | archived
            $table->timestamps();

            $table->index(['user_id', 'next_service_due_on']);
            $table->index(['user_id', 'status']);
            $table->index(['user_id', 'serial']);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('installations');
    }
};
