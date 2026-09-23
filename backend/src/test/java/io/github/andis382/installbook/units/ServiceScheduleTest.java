package io.github.andis382.installbook.units;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.andis382.installbook.units.ServiceSchedule.ServiceState;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ServiceScheduleTest {

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    @Nested
    class Warranty {

        @Test
        void runsToTheAnniversaryOfTheInstall() {
            assertThat(ServiceSchedule.warrantyUntil(d("2025-10-12"), 24)).isEqualTo(d("2027-10-12"));
        }

        @Test
        void aMonthEndInstallEndsOnTheLastDayOfAShorterMonth() {
            assertThat(ServiceSchedule.warrantyUntil(d("2025-01-31"), 1)).isEqualTo(d("2025-02-28"));
            assertThat(ServiceSchedule.warrantyUntil(d("2023-01-31"), 13)).isEqualTo(d("2024-02-29"));
            assertThat(ServiceSchedule.warrantyUntil(d("2025-08-31"), 30)).isEqualTo(d("2028-02-29"));
        }

        @Test
        void aLeapDayInstallIsCoveredToTheTwentyEighthInCommonYears() {
            assertThat(ServiceSchedule.warrantyUntil(d("2024-02-29"), 12)).isEqualTo(d("2025-02-28"));
            assertThat(ServiceSchedule.warrantyUntil(d("2024-02-29"), 24)).isEqualTo(d("2026-02-28"));
            assertThat(ServiceSchedule.warrantyUntil(d("2024-02-29"), 48)).isEqualTo(d("2028-02-29"));
        }

        @Test
        void isActiveUpToAndIncludingItsLastDay() {
            LocalDate until = d("2027-10-12");
            assertThat(ServiceSchedule.warrantyActive(until, d("2027-10-11"))).isTrue();
            assertThat(ServiceSchedule.warrantyActive(until, d("2027-10-12"))).isTrue();
            assertThat(ServiceSchedule.warrantyActive(until, d("2027-10-13"))).isFalse();
        }
    }

    @Nested
    class NextService {

        @Test
        void firstServiceIsOneIntervalAfterTheInstall() {
            assertThat(ServiceSchedule.nextServiceDue(d("2025-10-12"), null, 12)).isEqualTo(d("2026-10-12"));
        }

        @Test
        void countsFromTheLastServiceNotFromTheOldDueDate() {
            // serviced three weeks late: the next one is a year after the visit, not after the missed date
            assertThat(ServiceSchedule.nextServiceDue(d("2025-10-12"), d("2026-11-02"), 12)).isEqualTo(d("2027-11-02"));
            // serviced early
            assertThat(ServiceSchedule.nextServiceDue(d("2025-10-12"), d("2026-09-01"), 12)).isEqualTo(d("2027-09-01"));
        }

        @Test
        void ignoresAServiceDatedBeforeTheInstall() {
            assertThat(ServiceSchedule.nextServiceDue(d("2025-10-12"), d("2025-01-05"), 12)).isEqualTo(d("2026-10-12"));
        }

        @Test
        void handlesMonthEndsAndLeapDays() {
            assertThat(ServiceSchedule.nextServiceDue(d("2024-02-29"), null, 12)).isEqualTo(d("2025-02-28"));
            assertThat(ServiceSchedule.nextServiceDue(d("2025-03-31"), null, 6)).isEqualTo(d("2025-09-30"));
            assertThat(ServiceSchedule.nextServiceDue(d("2023-02-28"), d("2023-08-31"), 6)).isEqualTo(d("2024-02-29"));
        }

        @Test
        void aLongerIntervalIsRespected() {
            assertThat(ServiceSchedule.nextServiceDue(d("2025-05-10"), null, 24)).isEqualTo(d("2027-05-10"));
        }
    }

    @Nested
    class State {

        private final LocalDate due = d("2026-10-20");

        @Test
        void isOkBeforeTheLeadWindowOpens() {
            assertThat(ServiceSchedule.serviceState(due, d("2026-09-19"), 30)).isEqualTo(ServiceState.OK);
        }

        @Test
        void isDueFromTheReminderDayUpToTheDueDate() {
            assertThat(ServiceSchedule.reminderDate(due, 30)).isEqualTo(d("2026-09-20"));
            assertThat(ServiceSchedule.serviceState(due, d("2026-09-20"), 30)).isEqualTo(ServiceState.DUE_SOON);
            assertThat(ServiceSchedule.serviceState(due, d("2026-10-20"), 30)).isEqualTo(ServiceState.DUE_SOON);
        }

        @Test
        void isOverdueTheDayAfter() {
            assertThat(ServiceSchedule.serviceState(due, d("2026-10-21"), 30)).isEqualTo(ServiceState.OVERDUE);
        }

        @Test
        void countsDaysInBothDirections() {
            assertThat(ServiceSchedule.daysUntil(due, d("2026-10-10"))).isEqualTo(10);
            assertThat(ServiceSchedule.daysUntil(due, d("2026-10-25"))).isEqualTo(-5);
        }
    }

    @Nested
    class UnitCycle {

        private Unit unit(LocalDate installedOn) {
            Unit unit = new Unit(1L, null, UnitType.BOILER, "Vaillant", "token");
            unit.schedule(installedOn, 24, 12);
            return unit;
        }

        @Test
        void aServiceMovesTheNextDueDateForward() {
            Unit unit = unit(d("2025-10-12"));
            unit.serviced(d("2026-10-20"));
            assertThat(unit.getLastServiceOn()).isEqualTo(d("2026-10-20"));
            assertThat(unit.getNextServiceDue()).isEqualTo(d("2027-10-20"));
            assertThat(unit.getWarrantyUntil()).isEqualTo(d("2027-10-12"));
        }

        @Test
        void aBackFilledOlderServiceChangesNothing() {
            Unit unit = unit(d("2024-10-12"));
            unit.serviced(d("2026-10-20"));
            unit.serviced(d("2025-10-15"));
            assertThat(unit.getNextServiceDue()).isEqualTo(d("2027-10-20"));
        }

        @Test
        void editingTheInstallDateRecomputesBothDates() {
            Unit unit = unit(d("2025-10-12"));
            unit.schedule(d("2025-09-01"), 36, 6);
            assertThat(unit.getWarrantyUntil()).isEqualTo(d("2028-09-01"));
            assertThat(unit.getNextServiceDue()).isEqualTo(d("2026-03-01"));
        }
    }
}
