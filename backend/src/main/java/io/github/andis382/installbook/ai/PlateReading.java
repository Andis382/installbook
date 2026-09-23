package io.github.andis382.installbook.ai;

/**
 * What Claude read off a unit's data plate. Every field is exactly as printed, or null when
 * it is not on the plate or cannot be read; serial characters are never guessed.
 *
 * @param type one of BOILER, AIR_CONDITIONER, HEAT_PUMP, WATER_HEATER, SOLAR_INVERTER, ALARM_PANEL, OTHER
 */
public record PlateReading(String type, String brand, String model, String serialNumber, String notes) {}
