package io.github.andis382.installbook.bookings;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.messaging.MessagesController.MessageView;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitType;
import io.github.andis382.installbook.visits.VisitService.VisitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingsController {

    private static final int CLOSED_LIMIT = 60;

    private final BookingRequestRepository bookings;
    private final BookingService service;
    private final CustomerNotifier notifier;
    private final Installers installers;
    private final CurrentUser currentUser;

    public BookingsController(BookingRequestRepository bookings, BookingService service, CustomerNotifier notifier,
                              Installers installers, CurrentUser currentUser) {
        this.bookings = bookings;
        this.service = service;
        this.notifier = notifier;
        this.installers = installers;
        this.currentUser = currentUser;
    }

    public record BookingView(Long id, String status, String source, LocalDate preferredDate, String preferredPeriod,
                              String note, Instant createdAt, Instant scheduledAt, Instant decidedAt, boolean fromReminder,
                              Long visitId, Long unitId, UnitType unitType, String brand, String model, String serialNumber,
                              String address, LocalDate nextServiceDue, Long customerId, String customerName,
                              String customerPhone, boolean whatsappOptIn) {

        public static BookingView of(BookingRequest b) {
            Unit u = b.getUnit();
            return new BookingView(b.getId(), b.getStatus().name(), b.getSource().name(), b.getPreferredDate(),
                b.getPreferredPeriod() == null ? null : b.getPreferredPeriod().name(), b.getNote(), b.getCreatedAt(),
                b.getScheduledAt(), b.getDecidedAt(), b.getReminderId() != null, b.getVisitId(), u.getId(), u.getType(),
                u.getBrand(), u.getModel(), u.getSerialNumber(), u.getAddress(), u.getNextServiceDue(),
                u.getCustomer().getId(), u.getCustomer().getName(), u.getCustomer().getPhone(),
                u.getCustomer().isWhatsappOptIn());
        }
    }

    public record BookingList(List<BookingView> items, Map<BookingRequest.Status, Long> counts) {}

    public record ScheduleRequest(@NotNull LocalDate date, @NotNull LocalTime time, boolean notifyCustomer) {}

    public record DeclineRequest(boolean notifyCustomer, @Size(max = 300) String reason) {}

    /** The booking after a change, the message it sent (if any) and a wa.me link when it could not send one. */
    public record ActionResult(BookingView booking, MessageView message, String shareUrl) {}

    @GetMapping
    @Transactional(readOnly = true)
    public BookingList list(@RequestParam(defaultValue = "NEW") BookingRequest.Status status) {
        Long org = currentUser.organizationId();
        List<BookingView> items = bookings.findWithUnit(org, EnumSet.of(status)).stream()
            .limit(status.isOpen() ? Long.MAX_VALUE : CLOSED_LIMIT)
            .map(BookingView::of)
            .toList();
        if (status == BookingRequest.Status.SCHEDULED) {
            items = items.stream().sorted((a, b) -> a.scheduledAt().compareTo(b.scheduledAt())).toList();
        }
        Map<BookingRequest.Status, Long> counts = new EnumMap<>(BookingRequest.Status.class);
        for (BookingRequest.Status s : BookingRequest.Status.values()) {
            counts.put(s, bookings.countByOrganizationIdAndStatus(org, s));
        }
        return new BookingList(items, counts);
    }

    @PostMapping("/{id}/schedule")
    @Transactional
    public ActionResult schedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest req) {
        Organization org = currentUser.organization();
        BookingRequest booking = find(id);
        Optional<OutboundMessage> sent = service.schedule(booking, req.date(), req.time(), req.notifyCustomer(), org);
        String share = req.notifyCustomer() && sent.isEmpty() ? notifier.bookingScheduledShareUrl(booking, org) : null;
        return new ActionResult(BookingView.of(booking), sent.map(MessageView::of).orElse(null), share);
    }

    @PostMapping("/{id}/done")
    @Transactional
    public ActionResult done(@PathVariable Long id, @Valid @RequestBody VisitRequest req) {
        Organization org = currentUser.organization();
        BookingRequest booking = find(id);
        service.done(booking, req, currentUser.id(), installers.today(org));
        return new ActionResult(BookingView.of(booking), null, null);
    }

    @PostMapping("/{id}/decline")
    @Transactional
    public ActionResult decline(@PathVariable Long id, @Valid @RequestBody DeclineRequest req) {
        BookingRequest booking = find(id);
        Optional<OutboundMessage> sent = service.decline(booking, req.notifyCustomer(), req.reason());
        return new ActionResult(BookingView.of(booking), sent.map(MessageView::of).orElse(null), null);
    }

    private BookingRequest find(Long id) {
        return bookings.findInOrganization(id, currentUser.organizationId()).orElseThrow(ApiException::notFound);
    }
}
