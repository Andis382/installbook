<?php

namespace Database\Seeders;

use App\Enums\ApplianceType;
use App\Models\ServiceVisit;
use App\Models\User;
use App\Services\InstallationRegistrar;
use Illuminate\Database\Seeder;
use Illuminate\Support\Carbon;

/**
 * A worked example: one installer, a year of work behind him, so that a clean clone shows
 * a dashboard with things overdue, things due soon, units waiting to be registered and a
 * card that can be opened, instead of an empty screen that proves nothing.
 */
class DatabaseSeeder extends Seeder
{
    public function run(): void
    {
        $registrar = app(InstallationRegistrar::class);

        $user = User::firstOrCreate(
            ['email' => 'demo@installbook.test'],
            [
                'name' => 'Arben Hoxha',
                'business_name' => 'Hoxha Termoteknikë',
                'phone' => '+355691234567',
                'city' => 'Tiranë',
                'locale' => 'sq',
                'timezone' => 'Europe/Tirane',
                'password' => 'password',
                'card_footer' => 'Faleminderit që zgjodhët punën tonë.',
            ]
        );

        if ($user->installations()->exists()) {
            $this->command?->info('Demo data already present.');

            return;
        }

        $today = Carbon::today();

        // [months ago, type, brand, model, serial, customer, phone, address, locale]
        $rows = [
            [13, ApplianceType::GasBoiler, 'Vaillant', 'ecoTEC plus 24', 'VA24-118837-2025', 'Miranda Leka', '0692001122', 'Rruga e Kavajës 12', 'sq'],
            [12, ApplianceType::SplitAc, 'Daikin', 'FTXM35R', 'D35R-9J10428', 'Besnik Çela', '0684455667', 'Rruga Myslym Shyri 42', 'sq'],
            [11, ApplianceType::GasBoiler, 'Ariston', 'Clas One 24', 'AR-CL1-772104', 'Fatjona Dervishi', '0695566778', 'Rruga Dervish Hima 3', 'sq'],
            [10, ApplianceType::WaterHeater, 'Ariston', 'Velis Evo 80', 'VE80-4410927', 'Klodian Prifti', '0672233445', 'Rruga Bardhyl 18', 'sq'],
            [8, ApplianceType::SolarPv, 'Huawei', 'SUN2000-5KTL', 'HW5K-2026-33192', 'Gentian Rroshi', '0699988776', 'Rruga e Elbasanit 90', 'sq'],
            [6, ApplianceType::MultiSplitAc, 'Mitsubishi', 'MXZ-3F54VF', 'MX3F-88120455', 'Elena Rossi', '00393331234567', 'Rruga Sami Frashëri 7', 'en'],
            [2, ApplianceType::HeatPump, 'Panasonic', 'Aquarea J 9kW', 'PAQ9-7710233', 'Ilir Manushi', '0681122334', 'Rruga Jordan Misja 22', 'sq'],
            [0, ApplianceType::CondensingBoiler, 'Bosch', 'Condens 2000 W', 'BC2000-551038', 'Sara Kola', '0694433221', 'Rruga Qemal Stafa 55', 'sq'],
        ];

        foreach ($rows as [$monthsAgo, $type, $brand, $model, $serial, $name, $phone, $address, $locale]) {
            $installedOn = $today->copy()->subMonths($monthsAgo)->subDays(3);

            $install = $registrar->register($user, [
                'phone' => $phone,
                'customer_name' => $name,
                'address' => $address,
                'city' => 'Tiranë',
                'customer_locale' => $locale,
                'messaging_consent' => true,
                'appliance_type' => $type->value,
                'brand' => $brand,
                'model' => $model,
                'serial' => $serial,
                'installed_on' => $installedOn->toDateString(),
                'service_required_for_warranty' => in_array($type, [ApplianceType::GasBoiler, ApplianceType::CondensingBoiler], true),
                'location_note' => $type === ApplianceType::SolarPv ? 'Çati' : 'Kuzhinë',
                'send_card' => true,
            ]);

            // The oldest units have already been serviced once, so the second cycle shows.
            if ($monthsAgo >= 12) {
                $registrar->recordVisit($install, [
                    'kind' => ServiceVisit::KIND_SERVICE,
                    'performed_on' => $installedOn->copy()->addMonths(12)->toDateString(),
                    'notes' => 'Servis vjetor, pastrim dhe kontroll presioni.',
                    'price' => 45,
                ]);
            }

            // A couple of units are already registered with the manufacturer.
            if (in_array($monthsAgo, [10, 8], true)) {
                $install->update(['manufacturer_registered_at' => $installedOn->copy()->addDays(4)]);
            }
        }

        $this->command?->info('Seeded '.$user->installations()->count().' installs for demo@installbook.test / password');
    }
}
