package io.github.andis382.installbook.customers;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.auth.Organization;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.messaging.InboundMessageRepository;
import io.github.andis382.installbook.messaging.OutboundMessageRepository;
import io.github.andis382.installbook.settings.Installers;
import io.github.andis382.installbook.units.UnitDtos.CustomerSummary;
import io.github.andis382.installbook.units.UnitDtos.UnitRow;
import io.github.andis382.installbook.units.UnitRepository;
import io.github.andis382.installbook.units.UnitStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomersController {

    private static final int CONVERSATION_LIMIT = 60;

    private final CustomerRepository customers;
    private final CustomerService service;
    private final UnitRepository units;
    private final OutboundMessageRepository outbox;
    private final InboundMessageRepository inbox;
    private final Installers installers;
    private final CurrentUser currentUser;

    public CustomersController(CustomerRepository customers, CustomerService service, UnitRepository units,
                               OutboundMessageRepository outbox, InboundMessageRepository inbox, Installers installers,
                               CurrentUser currentUser) {
        this.customers = customers;
        this.service = service;
        this.units = units;
        this.outbox = outbox;
        this.inbox = inbox;
        this.installers = installers;
        this.currentUser = currentUser;
    }

    public record Lookup(boolean found, CustomerSummary customer, List<UnitRow> units) {}

    /** One line of the conversation with a customer, both directions merged. */
    public record ConversationItem(String direction, Instant at, String body, String status, String templateKey) {}

    public record CustomerDetail(Long id, String name, String phone, String locale, boolean whatsappOptIn,
                                 Instant whatsappOptInAt, Instant whatsappOptOutAt, String notes, Instant createdAt,
                                 List<UnitRow> units, List<ConversationItem> conversation) {}

    public record UpdateCustomerRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 40) String phone,
        @Pattern(regexp = "en|sq") String locale,
        @Size(max = 2000) String notes) {}

    public record ConsentRequest(@NotNull Boolean optIn) {}

    @GetMapping
    public List<CustomerRow> list(@RequestParam(required = false) String q, @RequestParam(defaultValue = "300") int limit) {
        String text = q == null ? "" : q.trim().toLowerCase();
        String digits = text.replaceAll("\\D", "");
        String local = digits.startsWith("0") ? digits.substring(1) : digits;
        return customers.search(currentUser.organizationId(), "%" + text + "%",
            digits.length() >= 3 ? "%" + local + "%" : "%" + text + "%",
            UnitStatus.ACTIVE, PageRequest.of(0, Math.min(Math.max(limit, 1), 500)));
    }

    /** For the install form: is this phone already a customer, and what did we fit for them? */
    @GetMapping("/lookup")
    @Transactional(readOnly = true)
    public Lookup lookup(@RequestParam String phone) {
        Organization org = currentUser.organization();
        return service.findByPhone(org.getId(), phone)
            .map(c -> new Lookup(true, CustomerSummary.of(c), rows(c, org)))
            .orElse(new Lookup(false, null, List.of()));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public CustomerDetail get(@PathVariable Long id) {
        return detail(find(id), currentUser.organization());
    }

    @PutMapping("/{id}")
    @Transactional
    public CustomerDetail update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerRequest req) {
        Customer c = service.update(find(id), req.name(), req.phone(), req.locale(), req.notes());
        return detail(c, currentUser.organization());
    }

    /** The installer records what the customer told him: yes to WhatsApp, or no more. */
    @PutMapping("/{id}/consent")
    @Transactional
    public CustomerDetail consent(@PathVariable Long id, @Valid @RequestBody ConsentRequest req) {
        Customer c = service.setConsent(find(id), req.optIn());
        return detail(c, currentUser.organization());
    }

    private CustomerDetail detail(Customer c, Organization org) {
        List<ConversationItem> conversation = new ArrayList<>();
        outbox.findByOrganizationIdAndRecipientOrderByCreatedAtDesc(org.getId(), c.getPhone(), PageRequest.of(0, CONVERSATION_LIMIT))
            .forEach(m -> conversation.add(new ConversationItem("OUT", m.getCreatedAt(), m.getBody(), m.getStatus().name(), m.getTemplateKey())));
        inbox.findByOrganizationIdAndFromPhoneOrderByReceivedAtDesc(org.getId(), c.getPhone(), PageRequest.of(0, CONVERSATION_LIMIT))
            .forEach(m -> conversation.add(new ConversationItem("IN", m.getReceivedAt(), m.getBody(), m.isHandled() ? "HANDLED" : "UNHANDLED", null)));
        conversation.sort(Comparator.comparing(ConversationItem::at).reversed());
        return new CustomerDetail(c.getId(), c.getName(), c.getPhone(), c.getLocale(), c.isWhatsappOptIn(), c.getWhatsappOptInAt(),
            c.getWhatsappOptOutAt(), c.getNotes(), c.getCreatedAt(), rows(c, org),
            conversation.stream().limit(CONVERSATION_LIMIT).toList());
    }

    private List<UnitRow> rows(Customer c, Organization org) {
        LocalDate today = installers.today(org);
        int lead = installers.settings(org.getId()).getReminderLeadDays();
        return units.findByCustomerIdOrderByInstalledOnDesc(c.getId()).stream().map(u -> UnitRow.of(u, today, lead)).toList();
    }

    private Customer find(Long id) {
        return customers.findByIdAndOrganizationId(id, currentUser.organizationId()).orElseThrow(ApiException::notFound);
    }
}
