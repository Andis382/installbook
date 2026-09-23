package io.github.andis382.installbook.demo;

import io.github.andis382.installbook.units.UnitType;
import java.util.List;
import java.util.Random;

/** Believable raw material for the demo register: people, streets, and the units an Albanian installer fits. */
final class DemoCatalog {

    private DemoCatalog() {}

    static final List<String> MEN = List.of("Arben", "Besnik", "Dritan", "Elton", "Gentian", "Ilir", "Klodian", "Luan",
        "Marsel", "Nertil", "Olsi", "Petrit", "Redon", "Sokol", "Ylli", "Artan", "Fatmir", "Gëzim", "Blerim", "Edmond",
        "Ermal", "Fation", "Genci", "Jetmir", "Kujtim", "Lorenc", "Mirel", "Orges", "Saimir", "Skënder", "Taulant",
        "Agron", "Bujar", "Erion", "Erjon", "Alban", "Denis", "Florian", "Julian", "Rezart");

    static final List<String> WOMEN = List.of("Arta", "Blerina", "Dorina", "Elona", "Entela", "Fjona", "Greta", "Ilda",
        "Jonida", "Klea", "Lindita", "Mimoza", "Nora", "Orjana", "Rudina", "Sidorela", "Teuta", "Valbona", "Xhensila",
        "Anila", "Brunilda", "Diana", "Eralda", "Esmeralda", "Flutura", "Gerta", "Irena", "Kejsi", "Lediana", "Majlinda",
        "Mirela", "Oriola", "Rovena", "Sonila", "Vjollca", "Ina", "Ajola", "Edlira");

    static final List<String> SURNAMES = List.of("Kola", "Dema", "Shehu", "Krasniqi", "Gashi", "Berisha", "Leka", "Marku",
        "Doda", "Basha", "Çela", "Duka", "Muça", "Lika", "Topi", "Prifti", "Xhafa", "Bregu", "Dervishi", "Hysa",
        "Kapllani", "Laçi", "Mema", "Ndreu", "Osmani", "Pepa", "Sula", "Tafa", "Vata", "Zeneli", "Brahimi", "Cani",
        "Gjoni", "Halili", "Isufi", "Karaj", "Lamaj", "Mulla", "Nikolla", "Rrapaj", "Shkurti", "Troka", "Veliu", "Zogaj",
        "Bardhi", "Qirjazi", "Myrtaj", "Hasani", "Alia", "Kuka");

    record Town(String name, double lat, double lng, double spread, List<String> streets) {}

    static final List<Town> TOWNS = List.of(
        new Town("Tiranë", 41.3275, 19.8187, 0.022, List.of("Rruga Myslym Shyri", "Rruga e Kavajës", "Rruga Sami Frashëri",
            "Rruga Ismail Qemali", "Rruga Pjetër Bogdani", "Rruga Asim Vokshi", "Rruga Hoxha Tahsim", "Rruga Qemal Stafa",
            "Rruga Siri Kodra", "Rruga Frosina Plaku", "Rruga Irfan Tomini", "Rruga Muhamet Gjollesha", "Rruga Bardhyl",
            "Rruga e Elbasanit", "Rruga Todi Shkurti", "Rruga Ali Demi", "Rruga Pasho Hysa", "Rruga Medar Shtylla",
            "Rruga Hamdi Sina", "Rruga Faik Konica", "Rruga Mihal Grameno", "Rruga e Durrësit", "Rruga Dibrës",
            "Rruga Njazi Meka")),
        new Town("Durrës", 41.3231, 19.4414, 0.014, List.of("Rruga Taulantia", "Rruga Egnatia", "Rruga Aleksandër Goga",
            "Rruga Skënderbeu", "Rruga Prokop Myzeqari", "Rruga Dhimitër Frashëri", "Rruga Pavarësia", "Rruga Ismail Qemali")),
        new Town("Elbasan", 41.1125, 20.0822, 0.012, List.of("Bulevardi Aleksandër Xhuvani", "Rruga Qemal Stafa",
            "Rruga Rinia", "Rruga Sulejman Pasha", "Rruga Kozma Naska", "Rruga Ismail Shehu")),
        new Town("Kamëz", 41.3817, 19.7602, 0.012, List.of("Rruga Blu", "Rruga Adem Jashari", "Rruga Isa Boletini",
            "Rruga Bajram Curri", "Rruga Hasan Prishtina")));

    /** Weights for picking a town: most work is in Tirana. */
    static final int[] TOWN_WEIGHTS = {62, 16, 10, 12};

    record Model(String brand, String model, int warrantyMonths, SerialFormat serial) {}

    @FunctionalInterface
    interface SerialFormat {
        String make(Random random, int year, int week);
    }

    private static String digits(Random r, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    private static String letters(Random r, int n) {
        String alphabet = "ABCDEFGHJKLMNPRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static final SerialFormat VAILLANT = (r, y, w) -> "21" + (y % 100) + String.format("%02d", w) + "00100" + digits(r, 6) + "N" + r.nextInt(10);
    private static final SerialFormat ARISTON = (r, y, w) -> (y % 100) + String.format("%02d", w) + digits(r, 9);
    private static final SerialFormat BAXI = (r, y, w) -> (y % 100) + String.format("%02d", w) + "U" + digits(r, 7);
    private static final SerialFormat BOSCH = (r, y, w) -> "7736901" + digits(r, 3) + " " + digits(r, 6);
    private static final SerialFormat IMMERGAS = (r, y, w) -> (y % 100) + "U" + String.format("%02d", w) + digits(r, 7);
    private static final SerialFormat FERROLI = (r, y, w) -> (y % 100) + String.format("%02d", w) + "L" + digits(r, 6);
    private static final SerialFormat DAIKIN = (r, y, w) -> "E" + digits(r, 7);
    private static final SerialFormat GREE = (r, y, w) -> "4" + letters(r, 1) + digits(r, 2) + letters(r, 1) + digits(r, 7);
    private static final SerialFormat MITSUBISHI = (r, y, w) -> (y % 10) + String.format("%02d", w) + digits(r, 5) + letters(r, 1);
    private static final SerialFormat LG = (r, y, w) -> (y % 10) + String.format("%02d", w) + "KA" + letters(r, 2) + digits(r, 5);
    private static final SerialFormat HUAWEI = (r, y, w) -> "HV" + (y % 100) + letters(r, 2) + digits(r, 7);
    private static final SerialFormat FRONIUS = (r, y, w) -> "3" + digits(r, 7);
    private static final SerialFormat AJAX = (r, y, w) -> digits(r, 2) + letters(r, 2) + digits(r, 4) + letters(r, 1);
    private static final SerialFormat PARADOX = (r, y, w) -> digits(r, 3) + "E" + letters(r, 1) + digits(r, 3);
    private static final SerialFormat GRUNDFOS = (r, y, w) -> "P" + (y % 100) + String.format("%02d", w) + digits(r, 5);

    static final List<Model> BOILERS = List.of(
        new Model("Vaillant", "ecoTEC plus VU 246/5-5", 24, VAILLANT),
        new Model("Vaillant", "ecoTEC pro VUW 236/5-3", 24, VAILLANT),
        new Model("Ariston", "Clas One 24", 24, ARISTON),
        new Model("Ariston", "Genus One 30", 24, ARISTON),
        new Model("Baxi", "Luna Duo-tec E 24", 24, BAXI),
        new Model("Baxi", "Prime 26", 24, BAXI),
        new Model("Bosch", "Condens 2300i W 24", 24, BOSCH),
        new Model("Immergas", "Victrix Tera 24", 24, IMMERGAS),
        new Model("Ferroli", "Bluehelix Tech RRT 24C", 24, FERROLI),
        new Model("Ferroli", "Divacondens D 28", 24, FERROLI));

    static final List<Model> AIR_CONDITIONERS = List.of(
        new Model("Daikin", "FTXM35R Perfera", 36, DAIKIN),
        new Model("Daikin", "FTXC25D Sensira", 36, DAIKIN),
        new Model("Gree", "GWH12AAB Pular", 36, GREE),
        new Model("Gree", "GWH18ACD Fairy", 36, GREE),
        new Model("Mitsubishi Electric", "MSZ-AP25VG", 36, MITSUBISHI),
        new Model("Mitsubishi Electric", "MSZ-LN35VG", 36, MITSUBISHI),
        new Model("LG", "S12ET Dualcool", 36, LG),
        new Model("LG", "AC12BK Artcool", 36, LG));

    static final List<Model> HEAT_PUMPS = List.of(
        new Model("Daikin", "Altherma 3 R ERGA06DV", 60, DAIKIN),
        new Model("Mitsubishi Electric", "Ecodan PUZ-WM50VHA", 60, MITSUBISHI),
        new Model("Vaillant", "aroTHERM plus VWL 75/6", 60, VAILLANT));

    static final List<Model> WATER_HEATERS = List.of(
        new Model("Ariston", "Velis Evo 80", 24, ARISTON),
        new Model("Ariston", "Lydos Eco 100", 24, ARISTON),
        new Model("Ferroli", "Calypso 80", 24, FERROLI),
        new Model("Bosch", "Tronic 2000T 80", 24, BOSCH));

    static final List<Model> SOLAR_INVERTERS = List.of(
        new Model("Huawei", "SUN2000-5KTL-L1", 60, HUAWEI),
        new Model("Fronius", "Primo 5.0-1", 60, FRONIUS));

    static final List<Model> ALARM_PANELS = List.of(
        new Model("Ajax", "Hub 2 Plus", 24, AJAX),
        new Model("Paradox", "SP6000+", 24, PARADOX));

    static final List<Model> OTHERS = List.of(new Model("Grundfos", "ALPHA2 25-60", 24, GRUNDFOS));

    static List<Model> models(UnitType type) {
        return switch (type) {
            case BOILER -> BOILERS;
            case AIR_CONDITIONER -> AIR_CONDITIONERS;
            case HEAT_PUMP -> HEAT_PUMPS;
            case WATER_HEATER -> WATER_HEATERS;
            case SOLAR_INVERTER -> SOLAR_INVERTERS;
            case ALARM_PANEL -> ALARM_PANELS;
            case OTHER -> OTHERS;
        };
    }

    /** Parts and work the way the installer jots them down after a visit, in his own language. */
    static final List<String> SERVICE_PARTS = List.of("Kontroll i djegies, u pastrua shkëmbyesi",
        "U ndërrua elektroda e ndezjes", "Vaza e zgjerimit u mbush në 1 bar", "U pastrua sifoni i kondensës",
        "U pastrua filtri, u kontrollua presioni i gazit", "U ndërrua anoda e magnezit",
        "U lanë filtrat, u kontrollua presioni i R32", "U shpëla tubi i shkarkimit");

    static final List<String> REPAIR_PARTS = List.of("U ndërrua sensori i presionit", "U ndërrua motori i ventilatorit",
        "U ndërrua aktuatori i valvulës me tre kalime", "U ndërrua siguresa e pllakës elektronike", "U ndërrua termostati");

    static int pick(Random r, int[] weights) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int x = r.nextInt(total);
        for (int i = 0; i < weights.length; i++) {
            x -= weights[i];
            if (x < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }

    static <T> T any(Random r, List<T> list) {
        return list.get(r.nextInt(list.size()));
    }
}
