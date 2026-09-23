package io.github.andis382.installbook.notify;

import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.common.Texts;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitType;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * The small pieces customer messages are built from, in the customer's language:
 * "the Vaillant boiler" / "kaldaja Vaillant", "12 Oct 2025" / "12 tetor 2025".
 */
@Component
public class MessageTexts {

    private final Texts texts;

    public MessageTexts(Texts texts) {
        this.texts = texts;
    }

    /** "Vaillant boiler" (en) or "kaldaja Vaillant" (sq), lower-case, for use mid-sentence. */
    public String unit(Unit unit, String locale) {
        return unit(unit.getType(), unit.getBrand(), locale);
    }

    public String unit(UnitType type, String brand, String locale) {
        return texts.in(locale, "unit.phrase." + type.name()).replace("{brand}", brand).trim();
    }

    /** The same phrase starting a line: "Vaillant boiler" / "Kaldaja Vaillant". */
    public String unitCapitalized(Unit unit, String locale) {
        String phrase = unit(unit, locale);
        return phrase.isEmpty() ? phrase : Character.toUpperCase(phrase.charAt(0)) + phrase.substring(1);
    }

    /** Unit phrase plus serial when there is one: "Kaldaja Vaillant, nr. serial 21223300100". */
    public String unitLine(Unit unit, String locale) {
        String line = unitCapitalized(unit, locale);
        if (unit.getSerialNumber() != null && !unit.getSerialNumber().isBlank()) {
            line += ", " + texts.in(locale, "unit.serial_label") + " " + unit.getSerialNumber();
        }
        return line;
    }

    public String day(LocalDate date, String locale) {
        return DateTimeFormatter.ofPattern(texts.in(locale, "format.day"), locale(locale)).format(date);
    }

    public String month(LocalDate date, String locale) {
        return DateTimeFormatter.ofPattern("LLLL yyyy", locale(locale)).format(date);
    }

    public String dayAndTime(ZonedDateTime at, String locale) {
        return DateTimeFormatter.ofPattern(texts.in(locale, "format.day_time"), locale(locale)).format(at);
    }

    /** " (preferred: Tue 14 Oct, morning)" or "" when the customer left it open. */
    public String preference(BookingRequest booking, String locale) {
        if (booking.getPreferredDate() == null && booking.getPreferredPeriod() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (booking.getPreferredDate() != null) {
            sb.append(DateTimeFormatter.ofPattern(texts.in(locale, "format.weekday_day"), locale(locale))
                .format(booking.getPreferredDate()));
        }
        if (booking.getPreferredPeriod() != null && booking.getPreferredPeriod() != BookingRequest.Period.ANY) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(texts.in(locale, "booking.period." + booking.getPreferredPeriod().name()));
        }
        return sb.isEmpty() ? "" : " (" + texts.in(locale, "booking.preferred") + " " + sb + ")";
    }

    private static Locale locale(String locale) {
        return Locale.forLanguageTag(locale == null ? "sq" : locale);
    }
}
