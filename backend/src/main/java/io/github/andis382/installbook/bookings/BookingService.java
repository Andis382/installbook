package io.github.andis382.installbook.bookings;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.visits.ServiceVisit;
import io.github.andis382.installbook.visits.VisitService;
import io.github.andis382.installbook.visits.VisitService.VisitRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking requests from customers and what the installer does with them. A unit has at most
 * one open request: asking twice returns the first one instead of stacking up duplicates.
 */
@Service
public class BookingService {

    public static final Set<BookingRequest.Status> OPEN = EnumSet.of(BookingRequest.Status.NEW, BookingRequest.Status.SCHEDULED);
    private static final int MAX_DAYS_AHEAD = 92;

    private final BookingRequestRepository bookings;
    private final ReminderRepository reminders;
    private final VisitService visits;
    private final CustomerNotifier notifier;
    private final Clock clock;

    public BookingService(BookingRequestRepository bookings, ReminderRepository reminders, VisitService visits,
                          CustomerNotifier notifier, Clock clock) {
        this.bookings = bookings;
        this.reminders = reminders;
        this.visits = visits;
        this.notifier = notifier;
        this.clock = clock;
    }

    /** The request plus whether it was created now (false: the customer had already asked). */
    public record Created(BookingRequest booking, boolean created) {}

    @Transactional
    public Created request(Unit unit, BookingRequest.Source source, LocalDate preferredDate, BookingRequest.Period period,
                           String note, LocalDate today) {
        if (!unit.isActive()) {
            throw ApiException.conflict("unit.not_active");
        }
        if (preferredDate != null) {
            if (preferredDate.isBefore(today)) {
                throw ApiException.field("preferredDate", "booking.date_past");
            }
            if (preferredDate.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
                throw ApiException.field("preferredDate", "booking.date_too_far");
            }
        }
        Optional<BookingRequest> open = openFor(unit);
        if (open.isPresent()) {
            return new Created(open.get(), false);
        }
        BookingRequest booking = new BookingRequest(unit.getOrganizationId(), unit, source, preferredDate, period,
            note == null || note.isBlank() ? null : note.trim(), clock.instant());
        // A request that follows this cycle's reminder is the reminder working: count it.
        reminders.findByUnitIdAndDueOn(unit.getId(), unit.getNextServiceDue())
            .filter(r -> r.wentOut())
            .ifPresent(r -> {
                r.booked();
                booking.setReminderId(r.getId());
            });
        bookings.save(booking);
        notifier.bookingReceived(booking);
        return new Created(booking, true);
    }

    public Optional<BookingRequest> openFor(Unit unit) {
        return bookings.findFirstByUnitIdAndStatusInOrderByCreatedAtDesc(unit.getId(), OPEN);
    }

    /** Fixes the visit time and tells the customer (when they agreed to messages). */
    @Transactional
    public Optional<OutboundMessage> schedule(BookingRequest booking, LocalDate date, LocalTime time, boolean notify,
                                              Organization org) {
        Instant at = date.atTime(time).atZone(Installers.zone(org)).toInstant();
        if (at.isBefore(clock.instant())) {
            throw ApiException.field("date", "booking.time_past");
        }
        booking.schedule(at, clock.instant());
        return notify ? notifier.bookingScheduled(booking, org) : Optional.empty();
    }

    /** The visit happened: record it on the unit (which moves the cycle on for a service). */
    @Transactional
    public ServiceVisit done(BookingRequest booking, VisitRequest visit, Long userId, LocalDate today) {
        ServiceVisit recorded = visits.record(booking.getUnit(), visit, userId, today);
        booking.done(recorded.getId(), clock.instant());
        return recorded;
    }

    @Transactional
    public Optional<OutboundMessage> decline(BookingRequest booking, boolean notify, String reason) {
        booking.decline(clock.instant());
        return notify ? notifier.bookingDeclined(booking, reason) : Optional.empty();
    }
}
