package io.github.andis382.installbook.common;

/**
 * Phone numbers are stored in international format without "+" or spaces ("355691234567"),
 * which is what WhatsApp Cloud API and wa.me links expect.
 */
public final class Phones {

    private Phones() {}

    /**
     * Normalises what people actually type: "069 123 4567", "+355 69 123 4567", "00355691234567".
     * A leading 0 is replaced by the default country code.
     */
    public static String normalize(String raw, String defaultCountryCode) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        boolean plus = trimmed.startsWith("+");
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (plus) {
            return digits;
        }
        if (digits.startsWith("00")) {
            return digits.substring(2);
        }
        if (digits.startsWith("0")) {
            return defaultCountryCode + digits.substring(1);
        }
        if (digits.length() <= 9) {
            return defaultCountryCode + digits;
        }
        return digits;
    }

    public static boolean isPlausible(String normalized) {
        return normalized != null && normalized.length() >= 8 && normalized.length() <= 15;
    }

    /** "+355 69 123 4567" style for display (Albanian and Kosovar mobiles grouped, others as digits). */
    public static String display(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        if ((normalized.startsWith("355") && normalized.length() == 12) || (normalized.startsWith("383") && normalized.length() == 11)) {
            return "+" + normalized.substring(0, 3) + " " + normalized.substring(3, 5) + " " + normalized.substring(5, 8)
                + " " + normalized.substring(8);
        }
        return "+" + normalized;
    }
}
