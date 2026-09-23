package io.github.andis382.installbook.units;

/** What kind of machine was fitted. Drives the icon, the wording in messages and nothing else. */
public enum UnitType {
    BOILER,
    AIR_CONDITIONER,
    HEAT_PUMP,
    WATER_HEATER,
    SOLAR_INVERTER,
    ALARM_PANEL,
    OTHER;

    /** Lenient parse for AI output and imports: "air conditioner", "Air-Conditioner" -> AIR_CONDITIONER. */
    public static UnitType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toUpperCase().replaceAll("[^A-Z]+", "_");
        for (UnitType type : values()) {
            if (type.name().equals(key)) {
                return type;
            }
        }
        return null;
    }
}
