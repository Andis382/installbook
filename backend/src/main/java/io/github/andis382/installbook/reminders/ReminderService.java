package io.github.andis382.installbook.reminders;

import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingRequestRepository;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.ReminderPolicy.Decision;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The service-reminder run for one installer. Safe to repeat: the (unit, due date) pair is
 * unique and {@link ReminderPolicy} skips cycles that were handled, so running twice on the
 * same day sends nothing new.
 */
@Service
public class ReminderService {

    /** Namespaces the advisory lock so runs for the same organisation never overlap. */
    private static final long LOCK_NAMESPACE = 7_310_000_000L;
    private static final Set<BookingRequest.Status> OPEN = EnumSet.of(BookingRequest.Status.NEW, BookingRequest.Status.SCHEDULED);

    private final UnitRepository units;
    private final ReminderRepository reminders;
    private final BookingRequestRepository bookings;
    private final CustomerNotifier notifier;
    private final Installers installers;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ReminderService(UnitRepository units, ReminderRepository reminders, BookingRequestRepository bookings,
                           CustomerNotifier notifier, Installers installers, JdbcTemplate jdbc, Clock clock) {
        this.units = units;
        this.reminders = reminders;
        this.bookings = bookings;
        this.notifier = notifier;
        this.installers = installers;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public record RunResult(int sent, int noConsent, int failed, int skipped) {}

    @Transactional
    public RunResult run(Long organizationId, LocalDate today) {
        jdbc.queryForList("select pg_advisory_xact_lock(?)", LOCK_NAMESPACE + organizationId);
        int leadDays = installers.settings(organizationId).getReminderLeadDays();
        List<Unit> candidates = units.findByOrganizationIdAndStatusAndNextServiceDueLessThanEqualOrderByNextServiceDueAsc(
            organizationId, UnitStatus.ACTIVE, today.plusDays(leadDays));
        if (candidates.isEmpty()) {
            return new RunResult(0, 0, 0, 0);
        }
        List<Long> ids = candidates.stream().map(Unit::getId).toList();
        Map<Long, Unit> byId = candidates.stream().collect(Collectors.toMap(Unit::getId, Function.identity()));
        Map<Long, Reminder> thisCycle = reminders.findByUnitIdIn(ids).stream()
            .filter(r -> r.getDueOn().equals(byId.get(r.getUnitId()).getNextServiceDue()))
            .collect(Collectors.toMap(Reminder::getUnitId, Function.identity()));
        Set<Long> withOpenBooking = bookings.findByUnitIdInAndStatusIn(ids, OPEN).stream()
            .map(b -> b.getUnit().getId()).collect(Collectors.toSet());

        int sent = 0;
        int noConsent = 0;
        int failed = 0;
        int skipped = 0;
        for (Unit unit : candidates) {
            Reminder existing = thisCycle.get(unit.getId());
            Decision decision = ReminderPolicy.decide(unit.isActive(), unit.getNextServiceDue(), leadDays, today,
                unit.getCustomer().isWhatsappOptIn(), withOpenBooking.contains(unit.getId()),
                existing == null ? null : existing.getOutcome());
            switch (decision) {
                case SEND -> {
                    Reminder reminder = existing != null ? existing : newReminder(unit, Reminder.Trigger.AUTO);
                    if (deliver(unit, reminder, today)) {
                        sent++;
                    } else {
                        failed++;
                    }
                }
                case NO_CONSENT -> {
                    reminders.save(newReminder(unit, Reminder.Trigger.AUTO));
                    noConsent++;
                }
                default -> skipped++;
            }
        }
        return new RunResult(sent, noConsent, failed, skipped);
    }

    /** "Send reminder now" on a unit: for this cycle, whatever the calendar says. */
    @Transactional
    public Reminder sendNow(Unit unit, LocalDate today) {
        if (!unit.isActive()) {
            throw ApiException.conflict("unit.not_active");
        }
        if (!unit.getCustomer().isWhatsappOptIn()) {
            throw ApiException.conflict("customer.no_consent");
        }
        Reminder reminder = reminders.findByUnitIdAndDueOn(unit.getId(), unit.getNextServiceDue())
            .orElseGet(() -> newReminder(unit, Reminder.Trigger.MANUAL));
        deliver(unit, reminder, today);
        return reminder;
    }

    private boolean deliver(Unit unit, Reminder reminder, LocalDate today) {
        OutboundMessage message = notifier.serviceDue(unit, today);
        boolean ok = message.getStatus() != OutboundMessage.Status.FAILED;
        if (ok) {
            reminder.delivered(message.getId(), clock.instant());
        } else {
            reminder.failed(message.getId());
        }
        reminders.save(reminder);
        return ok;
    }

    private Reminder newReminder(Unit unit, Reminder.Trigger trigger) {
        return new Reminder(unit.getOrganizationId(), unit.getId(), unit.getNextServiceDue(), trigger, clock.instant());
    }
}
