package io.github.andis382.installbook.reminders;

import io.github.andis382.installbook.units.ServiceSchedule;
import java.time.LocalDate;

/**
 * Decides, for one unit on one day, what the reminder run does. Pure, so the rules are
 * readable and tested on their own:
 * <ul>
 *   <li>nothing before the lead window opens (due date minus lead days);</li>
 *   <li>nothing when the customer already asked for a visit;</li>
 *   <li>one reminder per due cycle, ever: a sent, failed, booked or serviced cycle is left alone;</li>
 *   <li>no WhatsApp without consent: that cycle is recorded once as a call for the installer,
 *       and sent later only if the customer opts in before the cycle ends.</li>
 * </ul>
 */
public final class ReminderPolicy {

    private ReminderPolicy() {}

    public enum Decision { INACTIVE, NOT_YET, BOOKING_OPEN, ALREADY_HANDLED, NO_CONSENT, SEND }

    /**
     * @param existing the outcome of this cycle's reminder, or null when there is none yet
     */
    public static Decision decide(boolean active, LocalDate nextServiceDue, int leadDays, LocalDate today,
                                  boolean optedIn, boolean bookingOpen, Reminder.Outcome existing) {
        if (!active) {
            return Decision.INACTIVE;
        }
        if (ServiceSchedule.reminderDate(nextServiceDue, leadDays).isAfter(today)) {
            return Decision.NOT_YET;
        }
        if (bookingOpen) {
            return Decision.BOOKING_OPEN;
        }
        if (existing != null && existing != Reminder.Outcome.NO_CONSENT) {
            return Decision.ALREADY_HANDLED;
        }
        if (!optedIn) {
            return existing == null ? Decision.NO_CONSENT : Decision.ALREADY_HANDLED;
        }
        return Decision.SEND;
    }
}
