package io.github.andis382.installbook.notify;

import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.auth.OrganizationRepository;
import io.github.andis382.installbook.bookings.BookingRequest;
import io.github.andis382.installbook.config.AppProperties;
import io.github.andis382.installbook.customers.Customer;
import io.github.andis382.installbook.messaging.Messenger;
import io.github.andis382.installbook.messaging.Messenger.Outgoing;
import io.github.andis382.installbook.messaging.OutboundMessage;
import io.github.andis382.installbook.messaging.TemplateRenderer;
import io.github.andis382.installbook.settings.InstallerSettings;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.Unit;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Every WhatsApp message a customer receives about their unit. Messages go out only to
 * customers who agreed to them; for everyone else the caller gets the same text as a
 * click-to-chat link, so the installer can send it from his own phone.
 */
@Service
public class CustomerNotifier {

    /** Every unit message is filed against the unit, so it shows in that unit's history. */
    public static final String RELATED_UNIT = "UNIT";
    public static final String RELATED_CUSTOMER = "CUSTOMER";

    private final Messenger messenger;
    private final TemplateRenderer renderer;
    private final MessageTexts texts;
    private final OrganizationRepository organizations;
    private final Installers installers;
    private final AppProperties props;

    public CustomerNotifier(Messenger messenger, TemplateRenderer renderer, MessageTexts texts,
                            OrganizationRepository organizations, Installers installers, AppProperties props) {
        this.messenger = messenger;
        this.renderer = renderer;
        this.texts = texts;
        this.organizations = organizations;
        this.installers = installers;
        this.props = props;
    }

    /** A rendered message that has not been sent: its text as a wa.me link. */
    private record Draft(String template, Map<String, String> params, String link) {}

    public String cardLink(Unit unit) {
        return props.link("/c/" + unit.getCardToken());
    }

    public Optional<OutboundMessage> warrantyCard(Unit unit) {
        return sendIfConsented(unit, warrantyCardDraft(unit));
    }

    public String warrantyCardShareUrl(Unit unit) {
        return shareUrl(unit, warrantyCardDraft(unit));
    }

    /** The service reminder. Callers check consent first: without it the reminder is a phone call. */
    public OutboundMessage serviceDue(Unit unit, LocalDate today) {
        return send(unit, serviceDueDraft(unit, today));
    }

    public String serviceDueShareUrl(Unit unit, LocalDate today) {
        return shareUrl(unit, serviceDueDraft(unit, today));
    }

    public Optional<OutboundMessage> bookingReceived(BookingRequest booking) {
        Unit unit = booking.getUnit();
        Map<String, String> params = base(unit);
        params.put("preferred", texts.preference(booking, unit.getCustomer().getLocale()));
        return sendIfConsented(unit, new Draft("booking_received", params, null));
    }

    public Optional<OutboundMessage> bookingScheduled(BookingRequest booking, Organization org) {
        return sendIfConsented(booking.getUnit(), bookingScheduledDraft(booking, org));
    }

    public String bookingScheduledShareUrl(BookingRequest booking, Organization org) {
        return shareUrl(booking.getUnit(), bookingScheduledDraft(booking, org));
    }

    public Optional<OutboundMessage> bookingDeclined(BookingRequest booking, String reason) {
        Unit unit = booking.getUnit();
        Map<String, String> params = base(unit);
        params.put("reason", reason == null || reason.isBlank() ? "" : " (" + reason.trim() + ")");
        return sendIfConsented(unit, new Draft("booking_declined", params, null));
    }

    /** The one message that follows a withdrawal of consent: the confirmation of it. */
    public OutboundMessage optOutConfirmed(Customer customer) {
        Organization org = organizations.findById(customer.getOrganizationId()).orElseThrow();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("business", org.getName());
        return messenger.send(new Outgoing(org.getId(), customer.getPhone(), customer.getName(), "opt_out_confirmed",
            customer.getLocale(), params, null, RELATED_CUSTOMER, customer.getId()));
    }

    /** True when a provider accepted the message; false while it only sits in the outbox. */
    public static boolean delivered(OutboundMessage m) {
        return m.getStatus() == OutboundMessage.Status.SENT || m.getStatus() == OutboundMessage.Status.DELIVERED
            || m.getStatus() == OutboundMessage.Status.READ;
    }

    private Draft warrantyCardDraft(Unit unit) {
        String locale = unit.getCustomer().getLocale();
        Map<String, String> params = base(unit);
        params.put("unitLine", texts.unitLine(unit, locale));
        params.put("installed", texts.day(unit.getInstalledOn(), locale));
        params.put("warrantyUntil", texts.day(unit.getWarrantyUntil(), locale));
        return new Draft("warranty_card", params, cardLink(unit));
    }

    /** Due this month or later reads "is due in October"; a past month reads "was due", without alarm. */
    private Draft serviceDueDraft(Unit unit, LocalDate today) {
        String locale = unit.getCustomer().getLocale();
        Map<String, String> params = base(unit);
        params.put("installed", texts.day(unit.getInstalledOn(), locale));
        params.put("month", texts.month(unit.getNextServiceDue(), locale));
        boolean pastMonth = unit.getNextServiceDue().isBefore(today.withDayOfMonth(1));
        return new Draft(pastMonth ? "service_overdue" : "service_due", params, cardLink(unit));
    }

    private Draft bookingScheduledDraft(BookingRequest booking, Organization org) {
        Unit unit = booking.getUnit();
        Map<String, String> params = base(unit);
        params.put("when", texts.dayAndTime(booking.getScheduledAt().atZone(Installers.zone(org)), unit.getCustomer().getLocale()));
        params.put("address", unit.getAddress());
        return new Draft("booking_scheduled", params, cardLink(unit));
    }

    private Optional<OutboundMessage> sendIfConsented(Unit unit, Draft draft) {
        return unit.getCustomer().isWhatsappOptIn() ? Optional.of(send(unit, draft)) : Optional.empty();
    }

    private OutboundMessage send(Unit unit, Draft draft) {
        Customer c = unit.getCustomer();
        return messenger.send(new Outgoing(unit.getOrganizationId(), c.getPhone(), c.getName(), draft.template(),
            c.getLocale(), draft.params(), draft.link(), RELATED_UNIT, unit.getId()));
    }

    private String shareUrl(Unit unit, Draft draft) {
        Customer c = unit.getCustomer();
        Map<String, String> params = new LinkedHashMap<>(draft.params());
        if (draft.link() != null) {
            params.put("link", draft.link());
        }
        String text = renderer.render(draft.template(), c.getLocale(), params);
        return "https://wa.me/" + c.getPhone() + "?text=" + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private Map<String, String> base(Unit unit) {
        Organization org = organizations.findById(unit.getOrganizationId()).orElseThrow();
        InstallerSettings settings = installers.settings(org.getId());
        Customer c = unit.getCustomer();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("customer", c.firstName());
        params.put("business", org.getName());
        params.put("phone", installers.publicPhoneDisplay(org, settings));
        params.put("unit", texts.unit(unit, c.getLocale()));
        return params;
    }
}
