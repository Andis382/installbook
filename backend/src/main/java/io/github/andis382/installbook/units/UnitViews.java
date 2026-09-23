package io.github.andis382.installbook.units;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.User;
import io.github.andis382.installbook.auth.UserRepository;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.bookings.BookingRequestRepository;
import io.github.andis382.installbook.cards.CertificatePdf;
import io.github.andis382.installbook.messaging.InboundMessage;
import io.github.andis382.installbook.messaging.InboundMessageRepository;
import io.github.andis382.installbook.messaging.MessagesController.MessageView;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.messaging.OutboundMessageRepository;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.Reminder;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.UnitDtos.CustomerSummary;
import io.github.andis382.installbook.units.UnitDtos.CycleReminder;
import io.github.andis382.installbook.units.UnitDtos.OpenBooking;
import io.github.andis382.installbook.units.UnitDtos.TimelineEntry;
import io.github.andis382.installbook.units.UnitDtos.UnitDetail;
import io.github.andis382.installbook.units.UnitDtos.UnitRow;
import io.github.andis382.installbook.visits.ServiceVisit;
import io.github.andis382.installbook.visits.ServiceVisitRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Assembles the unit page: facts, the state of this cycle, and everything that ever happened to it. */
@Service
public class UnitViews {

    private static final Set<String> REMINDER_TEMPLATES = Set.of("service_due", "service_overdue");

    private final UnitRepository units;
    private final ServiceVisitRepository visits;
    private final ReminderRepository reminders;
    private final BookingRequestRepository bookings;
    private final OutboundMessageRepository outbox;
    private final InboundMessageRepository inbox;
    private final UserRepository users;
    private final Installers installers;
    private final CustomerNotifier notifier;

    public UnitViews(UnitRepository units, ServiceVisitRepository visits, ReminderRepository reminders,
                     BookingRequestRepository bookings, OutboundMessageRepository outbox, InboundMessageRepository inbox,
                     UserRepository users, Installers installers, CustomerNotifier notifier) {
        this.units = units;
        this.visits = visits;
        this.reminders = reminders;
        this.bookings = bookings;
        this.outbox = outbox;
        this.inbox = inbox;
        this.users = users;
        this.installers = installers;
        this.notifier = notifier;
    }

    @Transactional(readOnly = true)
    public UnitDetail detail(Unit unit, Organization org) {
        LocalDate today = installers.today(org);
        int lead = installers.settings(org.getId()).getReminderLeadDays();
        ZoneId zone = Installers.zone(org);
        Map<Long, String> names = users.findByOrganizationIdOrderByNameAsc(org.getId()).stream()
            .collect(Collectors.toMap(User::getId, User::getName));

        List<ServiceVisit> unitVisits = visits.findByUnitIdOrderByVisitedOnDescIdDesc(unit.getId());
        List<Reminder> unitReminders = reminders.findByUnitIdOrderByCreatedAtDesc(unit.getId());
        List<BookingRequest> unitBookings = bookings.findByUnitIdOrderByCreatedAtDesc(unit.getId());
        List<OutboundMessage> messages = outbox.findByOrganizationIdAndRelatedTypeAndRelatedIdOrderByCreatedAtDesc(
            org.getId(), CustomerNotifier.RELATED_UNIT, unit.getId());
        List<InboundMessage> replies = inbox.findByOrganizationIdAndRelatedTypeAndRelatedIdOrderByReceivedAtDesc(
            org.getId(), CustomerNotifier.RELATED_UNIT, unit.getId());

        CycleReminder cycle = unitReminders.stream()
            .filter(r -> r.getDueOn().equals(unit.getNextServiceDue()))
            .findFirst()
            .map(r -> new CycleReminder(r.getDueOn(), ServiceSchedule.reminderDate(r.getDueOn(), lead), r.getOutcome().name(), r.getSentAt()))
            .orElse(new CycleReminder(unit.getNextServiceDue(), ServiceSchedule.reminderDate(unit.getNextServiceDue(), lead), null, null));
        OpenBooking open = unitBookings.stream()
            .filter(b -> b.getStatus().isOpen())
            .findFirst()
            .map(b -> new OpenBooking(b.getId(), b.getStatus().name(), b.getPreferredDate(),
                b.getPreferredPeriod() == null ? null : b.getPreferredPeriod().name(), b.getScheduledAt(), b.getCreatedAt()))
            .orElse(null);
        MessageView lastCard = messages.stream()
            .filter(m -> "warranty_card".equals(m.getTemplateKey()))
            .findFirst().map(MessageView::of).orElse(null);
        List<UnitRow> others = units.findByCustomerIdOrderByInstalledOnDesc(unit.getCustomer().getId()).stream()
            .filter(u -> !u.getId().equals(unit.getId()))
            .map(u -> UnitRow.of(u, today, lead))
            .toList();

        return new UnitDetail(unit.getId(), unit.getType(), unit.getBrand(), unit.getModel(), unit.getSerialNumber(),
            unit.getStatus(), unit.getInstalledOn(), unit.getWarrantyMonths(), unit.getWarrantyUntil(),
            ServiceSchedule.warrantyActive(unit.getWarrantyUntil(), today), unit.getServiceIntervalMonths(),
            unit.getLastServiceOn(), unit.getNextServiceDue(),
            unit.isActive() ? ServiceSchedule.serviceState(unit.getNextServiceDue(), today, lead) : null,
            ServiceSchedule.daysUntil(unit.getNextServiceDue(), today), unit.getRemovedOn(), unit.getAddress(),
            unit.getLatitude(), unit.getLongitude(), unit.getNotes(), unit.getPlatePhotoId(),
            unit.getPlatePhotoId() == null ? null : "/api/files/" + unit.getPlatePhotoId(),
            unit.getInstalledBy() == null ? null : names.get(unit.getInstalledBy()), unit.getCreatedAt(),
            CertificatePdf.cardNumber(unit), notifier.cardLink(unit), notifier.warrantyCardShareUrl(unit), notifier.serviceDueShareUrl(unit, today),
            CustomerSummary.of(unit.getCustomer()), cycle, open, lastCard, others,
            timeline(unit, unitVisits, unitReminders, unitBookings, messages, replies, names, zone));
    }

    public MessageView message(Long messageId) {
        return messageId == null ? null : outbox.findById(messageId).map(MessageView::of).orElse(null);
    }

    /** Newest first. Date-only events sort at the moment they were entered when that was the same day. */
    static List<TimelineEntry> timeline(Unit unit, List<ServiceVisit> visits, List<Reminder> reminders,
                                        List<BookingRequest> bookings, List<OutboundMessage> messages,
                                        List<InboundMessage> replies, Map<Long, String> names, ZoneId zone) {
        record Dated(Instant sortKey, TimelineEntry entry) {}
        List<Dated> out = new ArrayList<>();
        Map<Long, OutboundMessage> byId = messages.stream().collect(Collectors.toMap(OutboundMessage::getId, Function.identity()));
        Set<Long> reminderMessages = reminders.stream().map(Reminder::getMessageId).filter(id -> id != null).collect(Collectors.toSet());

        out.add(new Dated(onDay(unit.getInstalledOn(), unit.getCreatedAt(), zone),
            entry("INSTALLED", null, unit.getInstalledOn()).by(names.get(unit.getInstalledBy())).build()));
        for (ServiceVisit v : visits) {
            out.add(new Dated(onDay(v.getVisitedOn(), v.getCreatedAt(), zone),
                entry("VISIT", null, v.getVisitedOn()).by(names.get(v.getPerformedBy())).visit(v).ref(v.getId()).build()));
        }
        for (Reminder r : reminders) {
            OutboundMessage m = r.getMessageId() == null ? null : byId.get(r.getMessageId());
            Instant at = r.getSentAt() != null ? r.getSentAt() : r.getCreatedAt();
            out.add(new Dated(at, entry("REMINDER", at, r.getDueOn()).outcome(r.getOutcome().name()).message(m).ref(r.getId()).build()));
        }
        for (BookingRequest b : bookings) {
            out.add(new Dated(b.getCreatedAt(), entry("BOOKING", b.getCreatedAt(), null).booking(b).ref(b.getId()).build()));
        }
        for (OutboundMessage m : messages) {
            if (reminderMessages.contains(m.getId()) && REMINDER_TEMPLATES.contains(m.getTemplateKey())) {
                continue;
            }
            out.add(new Dated(m.getCreatedAt(), entry("MESSAGE", m.getCreatedAt(), null).message(m).ref(m.getId()).build()));
        }
        for (InboundMessage in : replies) {
            out.add(new Dated(in.getReceivedAt(), entry("REPLY", in.getReceivedAt(), null).body(in.getBody()).ref(in.getId()).build()));
        }
        if (unit.getRemovedOn() != null) {
            out.add(new Dated(onDay(unit.getRemovedOn(), null, zone), entry("REMOVED", null, unit.getRemovedOn()).build()));
        }
        return out.stream()
            .sorted(Comparator.comparing(Dated::sortKey).reversed())
            .map(Dated::entry)
            .toList();
    }

    private static Instant onDay(LocalDate day, Instant enteredAt, ZoneId zone) {
        if (enteredAt != null && enteredAt.atZone(zone).toLocalDate().equals(day)) {
            return enteredAt;
        }
        return day.atTime(12, 0).atZone(zone).toInstant();
    }

    private static EntryBuilder entry(String kind, Instant at, LocalDate date) {
        return new EntryBuilder(kind, at, date);
    }

    /** Keeps the flat {@link TimelineEntry} constructor out of the timeline logic. */
    private static final class EntryBuilder {
        private final String kind;
        private final Instant at;
        private final LocalDate date;
        private String by;
        private String visitKind;
        private Integer priceCents;
        private String parts;
        private String notes;
        private String outcome;
        private String templateKey;
        private String body;
        private String messageStatus;
        private String source;
        private LocalDate preferredDate;
        private String preferredPeriod;
        private String bookingStatus;
        private Instant scheduledAt;
        private Long refId;

        EntryBuilder(String kind, Instant at, LocalDate date) {
            this.kind = kind;
            this.at = at;
            this.date = date;
        }

        EntryBuilder by(String name) {
            this.by = name;
            return this;
        }

        EntryBuilder visit(ServiceVisit v) {
            this.visitKind = v.getKind().name();
            this.priceCents = v.getPriceCents();
            this.parts = v.getParts();
            this.notes = v.getNotes();
            return this;
        }

        EntryBuilder outcome(String outcome) {
            this.outcome = outcome;
            return this;
        }

        EntryBuilder message(OutboundMessage m) {
            if (m != null) {
                this.templateKey = m.getTemplateKey();
                this.body = m.getBody();
                this.messageStatus = m.getStatus().name();
            }
            return this;
        }

        EntryBuilder body(String body) {
            this.body = body;
            return this;
        }

        EntryBuilder booking(BookingRequest b) {
            this.source = b.getSource().name();
            this.preferredDate = b.getPreferredDate();
            this.preferredPeriod = b.getPreferredPeriod() == null ? null : b.getPreferredPeriod().name();
            this.bookingStatus = b.getStatus().name();
            this.scheduledAt = b.getScheduledAt();
            this.notes = b.getNote();
            return this;
        }

        EntryBuilder ref(Long id) {
            this.refId = id;
            return this;
        }

        TimelineEntry build() {
            return new TimelineEntry(kind, at, date, by, visitKind, priceCents, parts, notes, outcome, templateKey, body,
                messageStatus, source, preferredDate, preferredPeriod, bookingStatus, scheduledAt, refId);
        }
    }
}
