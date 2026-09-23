package io.github.andis382.installbook.dashboard;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingRequestRepository;
import io.github.andis382.installbook.bookings.BookingsController.BookingView;
import io.github.andis382.installbook.reminders.Reminder;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.settings.InstallerSettings;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitDtos.UnitRow;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "Today" screen: who to call, what is due, what it is worth, and whether the reminders
 * are turning into bookings. Every number is a count of real records, nothing modelled.
 */
@Service
public class DashboardService {

    private static final Duration CONVERSION_WINDOW = Duration.ofDays(90);
    private static final int INSTALL_MONTHS = 12;

    private final UnitRepository units;
    private final BookingRequestRepository bookings;
    private final ReminderRepository reminders;
    private final Installers installers;
    private final Clock clock;

    public DashboardService(UnitRepository units, BookingRequestRepository bookings, ReminderRepository reminders,
                            Installers installers, Clock clock) {
        this.units = units;
        this.bookings = bookings;
        this.reminders = reminders;
        this.installers = installers;
        this.clock = clock;
    }

    public record Conversion(long sent, long booked, double rate) {}

    public record MonthCount(String month, long count) {}

    public record Dashboard(LocalDate today, List<BookingView> newBookings, long dueThisMonth, long overdue,
                            int typicalPriceCents, long revenueThisMonthCents, long revenueOverdueCents,
                            Conversion conversion, long installsThisMonth, long installsLastMonth,
                            List<MonthCount> installsByMonth, List<UnitRow> dueNext, long remindersThisWeek,
                            long activeUnits) {}

    @Transactional(readOnly = true)
    public Dashboard build(Organization org) {
        Long orgId = org.getId();
        LocalDate today = installers.today(org);
        InstallerSettings settings = installers.settings(orgId);
        int lead = settings.getReminderLeadDays();
        int price = settings.getTypicalServicePriceCents();
        YearMonth month = YearMonth.from(today);

        List<BookingView> fresh = bookings.findWithUnit(orgId, EnumSet.of(BookingRequest.Status.NEW)).stream()
            .map(BookingView::of).toList();
        long dueThisMonth = units.countByOrganizationIdAndStatusAndNextServiceDueBetween(orgId, UnitStatus.ACTIVE, today,
            month.atEndOfMonth());
        long overdue = units.countByOrganizationIdAndStatusAndNextServiceDueBefore(orgId, UnitStatus.ACTIVE, today);

        Instant since = clock.instant().minus(CONVERSION_WINDOW);
        long sent = reminders.countByOrganizationIdAndSentAtGreaterThanEqual(orgId, since);
        long booked = reminders.countByOrganizationIdAndSentAtGreaterThanEqualAndOutcome(orgId, since, Reminder.Outcome.BOOKED);

        List<Unit> soon = units.findByOrganizationIdAndStatusAndNextServiceDueBetweenOrderByNextServiceDueAsc(orgId,
            UnitStatus.ACTIVE, today, today.plusDays(14));
        // reminders go out lead days before the due date, so next week's are the units due lead+1..lead+7 days out
        long remindersThisWeek = units.countByOrganizationIdAndStatusAndNextServiceDueBetween(orgId, UnitStatus.ACTIVE,
            today.plusDays(lead + 1L), today.plusDays(lead + 7L));

        return new Dashboard(today, fresh, dueThisMonth, overdue, price, dueThisMonth * price, overdue * price,
            new Conversion(sent, booked, sent == 0 ? 0 : (double) booked / sent),
            units.countByOrganizationIdAndInstalledOnBetween(orgId, month.atDay(1), today),
            units.countByOrganizationIdAndInstalledOnBetween(orgId, month.minusMonths(1).atDay(1), month.minusMonths(1).atEndOfMonth()),
            installsByMonth(orgId, month),
            soon.stream().limit(6).map(u -> UnitRow.of(u, today, lead)).toList(),
            remindersThisWeek,
            units.countByOrganizationIdAndStatus(orgId, UnitStatus.ACTIVE));
    }

    private List<MonthCount> installsByMonth(Long orgId, YearMonth current) {
        YearMonth first = current.minusMonths(INSTALL_MONTHS - 1L);
        Map<YearMonth, Long> counts = new TreeMap<>();
        for (int i = 0; i < INSTALL_MONTHS; i++) {
            counts.put(first.plusMonths(i), 0L);
        }
        for (Unit u : units.findByOrganizationIdAndInstalledOnGreaterThanEqual(orgId, first.atDay(1))) {
            counts.merge(YearMonth.from(u.getInstalledOn()), 1L, Long::sum);
        }
        List<MonthCount> out = new ArrayList<>();
        counts.forEach((m, c) -> out.add(new MonthCount(m.toString(), c)));
        return out;
    }
}
