package io.github.andis382.installbook.units;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * The date arithmetic behind warranty cards and service reminders.
 *
 * <p>Both run on calendar months from a starting day. When the target month is shorter
 * (an install on 31 January, a leap-day install), the date falls on that month's last day
 * instead of spilling into the next month. The next service is always counted from the
 * latest real event, the install or the last service, so an early or late visit resets the
 * cycle rather than letting it drift.
 */
public final class ServiceSchedule {

    private ServiceSchedule() {}

    public enum ServiceState {
        /** The due date has passed. */
        OVERDUE,
        /** Due within the reminder lead window (today included). */
        DUE_SOON,
        /** Nothing to do yet. */
        OK
    }

    /** The last day the warranty covers: the anniversary of the install, inclusive. */
    public static LocalDate warrantyUntil(LocalDate installedOn, int warrantyMonths) {
        return installedOn.plusMonths(warrantyMonths);
    }

    public static boolean warrantyActive(LocalDate warrantyUntil, LocalDate today) {
        return !today.isAfter(warrantyUntil);
    }

    public static LocalDate nextServiceDue(LocalDate installedOn, LocalDate lastServiceOn, int intervalMonths) {
        LocalDate from = lastServiceOn != null && lastServiceOn.isAfter(installedOn) ? lastServiceOn : installedOn;
        return from.plusMonths(intervalMonths);
    }

    /** The day the automatic reminder becomes due: lead days before the service date. */
    public static LocalDate reminderDate(LocalDate nextServiceDue, int leadDays) {
        return nextServiceDue.minusDays(leadDays);
    }

    public static ServiceState serviceState(LocalDate nextServiceDue, LocalDate today, int leadDays) {
        if (nextServiceDue.isBefore(today)) {
            return ServiceState.OVERDUE;
        }
        return reminderDate(nextServiceDue, leadDays).isAfter(today) ? ServiceState.OK : ServiceState.DUE_SOON;
    }

    /** Negative when the date is in the past. */
    public static long daysUntil(LocalDate date, LocalDate today) {
        return ChronoUnit.DAYS.between(today, date);
    }
}
