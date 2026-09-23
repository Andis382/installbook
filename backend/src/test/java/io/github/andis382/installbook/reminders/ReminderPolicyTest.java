package io.github.andis382.installbook.reminders;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andis382.installbook.reminders.ReminderPolicy.Decision;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReminderPolicyTest {

    private static final LocalDate TODAY = LocalDate.parse("2026-09-23");
    private static final int LEAD = 30;

    private static Decision decide(LocalDate due, boolean optedIn, boolean bookingOpen, Reminder.Outcome existing) {
        return ReminderPolicy.decide(true, due, LEAD, TODAY, optedIn, bookingOpen, existing);
    }

    @Test
    void waitsUntilTheLeadWindowOpens() {
        assertThat(decide(TODAY.plusDays(31), true, false, null)).isEqualTo(Decision.NOT_YET);
        assertThat(decide(TODAY.plusDays(30), true, false, null)).isEqualTo(Decision.SEND);
    }

    @Test
    void sendsForOverdueUnitsThatWereNeverReminded() {
        assertThat(decide(TODAY.minusDays(40), true, false, null)).isEqualTo(Decision.SEND);
    }

    @Test
    void neverSendsTwiceInOneCycle() {
        for (Reminder.Outcome handled : new Reminder.Outcome[] {
            Reminder.Outcome.SENT, Reminder.Outcome.FAILED, Reminder.Outcome.BOOKED, Reminder.Outcome.SERVICED}) {
            assertThat(decide(TODAY.plusDays(10), true, false, handled)).isEqualTo(Decision.ALREADY_HANDLED);
        }
    }

    @Test
    void recordsMissingConsentOnceAndThenLeavesItAlone() {
        assertThat(decide(TODAY.plusDays(10), false, false, null)).isEqualTo(Decision.NO_CONSENT);
        assertThat(decide(TODAY.plusDays(10), false, false, Reminder.Outcome.NO_CONSENT)).isEqualTo(Decision.ALREADY_HANDLED);
    }

    @Test
    void sendsWhenTheCustomerOptsInLaterInTheSameCycle() {
        assertThat(decide(TODAY.plusDays(10), true, false, Reminder.Outcome.NO_CONSENT)).isEqualTo(Decision.SEND);
    }

    @Test
    void doesNotRemindSomeoneWhoAlreadyAskedForAVisit() {
        assertThat(decide(TODAY.plusDays(10), true, true, null)).isEqualTo(Decision.BOOKING_OPEN);
    }

    @Test
    void ignoresRemovedUnits() {
        assertThat(ReminderPolicy.decide(false, TODAY, LEAD, TODAY, true, false, null)).isEqualTo(Decision.INACTIVE);
    }
}
