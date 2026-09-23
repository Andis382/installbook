package io.github.andis382.installbook.customers;

import java.time.LocalDate;

/** A line in the customer list: who, how to reach them, and how many units they have with us. */
public record CustomerRow(Long id, String name, String phone, String locale, boolean whatsappOptIn, long activeUnits,
                          LocalDate lastInstalledOn) {}
