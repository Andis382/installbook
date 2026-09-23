package io.github.andis382.installbook.bookings;

import io.github.andis382.installbook.auth.OrganizationRepository;
import io.github.andis382.installbook.customers.Customer;
import io.github.andis382.installbook.customers.CustomerRepository;
import io.github.andis382.installbook.messaging.InboundHandler;
import io.github.andis382.installbook.messaging.InboundMessage;
import io.github.andis382.installbook.notify.CustomerNotifier;
import io.github.andis382.installbook.reminders.Reminder;
import io.github.andis382.installbook.reminders.ReminderRepository;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import io.github.andis382.installbook.units.UnitRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

/**
 * Customers answering a reminder on WhatsApp. "1", "po", "yes", "book", "rezervo" asks for a
 * service visit; "stop", "ndalo" withdraws consent. Anything else is left for a person to read.
 */
@Component
@Order(10)
public class ReplyHandler implements InboundHandler {

    static final Set<String> BOOK = Set.of("1", "po", "yes", "book", "rezervo", "rezervoj", "dua servis");
    static final Set<String> STOP = Set.of("stop", "ndalo", "ndal", "unsubscribe");
    /** A "1" this long after the last reminder is not an answer to it any more. */
    private static final Duration REMINDER_WINDOW = Duration.ofDays(60);

    private final CustomerRepository customers;
    private final UnitRepository units;
    private final ReminderRepository reminders;
    private final BookingService bookings;
    private final CustomerNotifier notifier;
    private final OrganizationRepository organizations;
    private final Installers installers;
    private final Clock clock;

    public ReplyHandler(CustomerRepository customers, UnitRepository units, ReminderRepository reminders,
                        BookingService bookings, CustomerNotifier notifier, OrganizationRepository organizations,
                        Installers installers, Clock clock) {
        this.customers = customers;
        this.units = units;
        this.reminders = reminders;
        this.bookings = bookings;
        this.notifier = notifier;
        this.organizations = organizations;
        this.installers = installers;
        this.clock = clock;
    }

    /** "Po!", " YES. ", "Rezervo" -> "po", "yes", "rezervo". Accents are kept (ë is a letter). */
    static String keyword(String body) {
        if (body == null) {
            return "";
        }
        String text = Normalizer.normalize(body, Normalizer.Form.NFC).toLowerCase().trim();
        return text.replaceAll("[\\p{Punct}\\s]+$", "").replaceAll("^[\\p{Punct}\\s]+", "").replaceAll("\\s+", " ");
    }

    @Override
    public boolean handle(InboundMessage message) {
        if (message.getOrganizationId() == null) {
            return false;
        }
        String word = keyword(message.getBody());
        boolean book = BOOK.contains(word);
        boolean stop = STOP.contains(word);
        if (!book && !stop) {
            return false;
        }
        Optional<Customer> found = customers.findByOrganizationIdAndPhone(message.getOrganizationId(), message.getFromPhone());
        if (found.isEmpty()) {
            return false;
        }
        Customer customer = found.get();
        if (stop) {
            customer.optOut(clock.instant());
            notifier.optOutConfirmed(customer);
            message.markHandled("opt-out", customer.getOrganizationId(), CustomerNotifier.RELATED_CUSTOMER, customer.getId());
            return true;
        }
        return unitToBook(customer).map(unit -> {
            var today = installers.today(organizations.findById(unit.getOrganizationId()).orElseThrow());
            var created = bookings.request(unit, BookingRequest.Source.WHATSAPP, null, BookingRequest.Period.ANY, null, today);
            if (!created.created()) {
                notifier.bookingReceived(created.booking());
            }
            message.markHandled("booking", customer.getOrganizationId(), CustomerNotifier.RELATED_UNIT, unit.getId());
            return true;
        }).orElse(false);
    }

    /** The unit of the reminder they are answering, else their unit that is due soonest. */
    private Optional<Unit> unitToBook(Customer customer) {
        Optional<Unit> reminded = reminders.findSentToCustomer(customer.getId(), clock.instant().minus(REMINDER_WINDOW), Limit.of(1))
            .stream().findFirst()
            .map(Reminder::getUnitId)
            .flatMap(units::findById)
            .filter(Unit::isActive);
        if (reminded.isPresent()) {
            return reminded;
        }
        return units.findByCustomerIdOrderByInstalledOnDesc(customer.getId()).stream()
            .filter(Unit::isActive)
            .min(Comparator.comparing(Unit::getNextServiceDue));
    }
}
