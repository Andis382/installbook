package io.github.andis382.installbook.due;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingRequestRepository;
import io.github.andis382.installbook.bookings.BookingService;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.Reminder;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.settings.InstallerSettings;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.ServiceSchedule;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitDtos.UnitRow;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The installer's work list: what is overdue, due this month and due next month, and for each
 * unit where the conversation with the customer stands.
 */
@Service
public class DueService {

    public enum Group { OVERDUE, THIS_MONTH, NEXT_MONTH }

    /** Where one unit stands between "due" and "booked", in the order the installer cares about. */
    public enum ReminderState { BOOKING_SCHEDULED, BOOKING_REQUESTED, SENT, NO_CONSENT, FAILED, WAITING, PLANNED }

    public record DueRow(UnitRow unit, ReminderState reminderState, Instant reminderSentAt, LocalDate reminderGoesOutOn,
                         Long bookingId, Instant bookingScheduledAt, boolean whatsappOptIn, String shareUrl) {}

    public record DueGroup(Group group, String month, List<DueRow> rows, long estimatedCents) {}

    public record DueView(LocalDate today, String month, String currentMonth, int typicalPriceCents, List<DueGroup> groups) {}

    private final UnitRepository units;
    private final ReminderRepository reminders;
    private final BookingRequestRepository bookings;
    private final CustomerNotifier notifier;
    private final Installers installers;

    public DueService(UnitRepository units, ReminderRepository reminders, BookingRequestRepository bookings,
                      CustomerNotifier notifier, Installers installers) {
        this.units = units;
        this.reminders = reminders;
        this.bookings = bookings;
        this.notifier = notifier;
        this.installers = installers;
    }

    /** Months before the current one are not browsable: what was missed then is in "overdue" now. */
    @Transactional(readOnly = true)
    public DueView view(Organization org, YearMonth requested) {
        LocalDate today = installers.today(org);
        YearMonth current = YearMonth.from(today);
        YearMonth month = requested == null || requested.isBefore(current) ? current : requested;
        InstallerSettings settings = installers.settings(org.getId());
        boolean isCurrent = month.equals(current);

        LocalDate monthStart = isCurrent ? today : month.atDay(1);
        List<Unit> overdue = isCurrent
            ? units.findByOrganizationIdAndStatusAndNextServiceDueLessThanEqualOrderByNextServiceDueAsc(org.getId(),
                UnitStatus.ACTIVE, today.minusDays(1))
            : List.of();
        List<Unit> thisMonth = between(org, monthStart, month.atEndOfMonth());
        YearMonth next = month.plusMonths(1);
        List<Unit> nextMonth = between(org, next.atDay(1), next.atEndOfMonth());

        List<Unit> all = new ArrayList<>(overdue);
        all.addAll(thisMonth);
        all.addAll(nextMonth);
        Context ctx = context(all, today, settings.getReminderLeadDays());
        int price = settings.getTypicalServicePriceCents();

        List<DueGroup> groups = new ArrayList<>();
        if (isCurrent) {
            groups.add(group(Group.OVERDUE, null, overdue, ctx, price));
        }
        groups.add(group(Group.THIS_MONTH, month.toString(), thisMonth, ctx, price));
        groups.add(group(Group.NEXT_MONTH, next.toString(), nextMonth, ctx, price));
        return new DueView(today, month.toString(), current.toString(), price, groups);
    }

    private List<Unit> between(Organization org, LocalDate from, LocalDate to) {
        return units.findByOrganizationIdAndStatusAndNextServiceDueBetweenOrderByNextServiceDueAsc(org.getId(),
            UnitStatus.ACTIVE, from, to);
    }

    private record Context(LocalDate today, int leadDays, Map<Long, Reminder> cycleReminders,
                           Map<Long, BookingRequest> openBookings) {}

    private Context context(List<Unit> all, LocalDate today, int leadDays) {
        if (all.isEmpty()) {
            return new Context(today, leadDays, Map.of(), Map.of());
        }
        List<Long> ids = all.stream().map(Unit::getId).toList();
        Map<Long, LocalDate> dueById = all.stream().collect(Collectors.toMap(Unit::getId, Unit::getNextServiceDue));
        Map<Long, Reminder> cycle = reminders.findByUnitIdIn(ids).stream()
            .filter(r -> r.getDueOn().equals(dueById.get(r.getUnitId())))
            .collect(Collectors.toMap(Reminder::getUnitId, Function.identity()));
        Map<Long, BookingRequest> open = bookings.findByUnitIdInAndStatusIn(ids, BookingService.OPEN).stream()
            .collect(Collectors.toMap(b -> b.getUnit().getId(), Function.identity(),
                (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b));
        return new Context(today, leadDays, cycle, open);
    }

    /** Soonest first; overdue units the other way round, so the ones that just slipped (still warm) lead. */
    private DueGroup group(Group group, String month, List<Unit> list, Context ctx, int price) {
        Comparator<Unit> byDue = Comparator.comparing(Unit::getNextServiceDue);
        List<DueRow> rows = list.stream()
            .sorted(group == Group.OVERDUE ? byDue.reversed() : byDue)
            .map(u -> row(u, ctx))
            .toList();
        return new DueGroup(group, month, rows, (long) rows.size() * price);
    }

    private DueRow row(Unit unit, Context ctx) {
        Reminder reminder = ctx.cycleReminders().get(unit.getId());
        BookingRequest booking = ctx.openBookings().get(unit.getId());
        LocalDate goesOut = ServiceSchedule.reminderDate(unit.getNextServiceDue(), ctx.leadDays());
        ReminderState state = state(reminder, booking, goesOut, ctx.today());
        boolean optIn = unit.getCustomer().isWhatsappOptIn();
        return new DueRow(UnitRow.of(unit, ctx.today(), ctx.leadDays()), state,
            reminder == null ? null : reminder.getSentAt(), goesOut,
            booking == null ? null : booking.getId(), booking == null ? null : booking.getScheduledAt(), optIn,
            notifier.serviceDueShareUrl(unit, ctx.today()));
    }

    static ReminderState state(Reminder reminder, BookingRequest booking, LocalDate goesOut, LocalDate today) {
        if (booking != null) {
            return booking.getStatus() == BookingRequest.Status.SCHEDULED ? ReminderState.BOOKING_SCHEDULED
                : ReminderState.BOOKING_REQUESTED;
        }
        if (reminder != null) {
            return switch (reminder.getOutcome()) {
                case NO_CONSENT -> ReminderState.NO_CONSENT;
                case FAILED -> ReminderState.FAILED;
                default -> ReminderState.SENT;
            };
        }
        return goesOut.isAfter(today) ? ReminderState.PLANNED : ReminderState.WAITING;
    }
}
