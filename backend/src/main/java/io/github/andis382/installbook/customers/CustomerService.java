package io.github.andis382.installbook.customers;

import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.Phones;
import io.github.andis382.installbook.config.AppProperties;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customers are found by phone: the number is what the installer has at the door. */
@Service
public class CustomerService {

    private final CustomerRepository customers;
    private final AppProperties props;
    private final Clock clock;

    public CustomerService(CustomerRepository customers, AppProperties props, Clock clock) {
        this.customers = customers;
        this.props = props;
        this.clock = clock;
    }

    /** Normalises what was typed ("069 123 4567") or fails on the given field. */
    public String phone(String raw, String field) {
        String normalized = Phones.normalize(raw, props.getDefaultCountryCode());
        if (!Phones.isPlausible(normalized)) {
            throw ApiException.field(field, "customer.phone_invalid");
        }
        return normalized;
    }

    public Optional<Customer> findByPhone(Long organizationId, String rawPhone) {
        String normalized = Phones.normalize(rawPhone, props.getDefaultCountryCode());
        if (!Phones.isPlausible(normalized)) {
            return Optional.empty();
        }
        return customers.findByOrganizationIdAndPhone(organizationId, normalized);
    }

    /**
     * The customer with this phone, created if new. An existing customer keeps their record;
     * the name and language are refreshed from what the installer just confirmed.
     * Consent is only ever added here, never withdrawn: withdrawing is its own explicit action.
     */
    @Transactional
    public Customer upsert(Long organizationId, String rawPhone, String name, String locale, boolean consent) {
        String phone = phone(rawPhone, "customerPhone");
        String lang = "en".equals(locale) ? "en" : "sq";
        Customer customer = customers.findByOrganizationIdAndPhone(organizationId, phone)
            .orElseGet(() -> new Customer(organizationId, name.trim(), phone, lang));
        customer.setName(name.trim());
        customer.setLocale(lang);
        if (consent) {
            customer.optIn(clock.instant());
        }
        return customers.save(customer);
    }

    @Transactional
    public Customer update(Customer customer, String name, String rawPhone, String locale, String notes) {
        String phone = phone(rawPhone, "phone");
        if (!phone.equals(customer.getPhone())) {
            customers.findByOrganizationIdAndPhone(customer.getOrganizationId(), phone).ifPresent(other -> {
                throw ApiException.field("phone", "customer.phone_taken");
            });
            customer.setPhone(phone);
        }
        customer.setName(name.trim());
        customer.setLocale("en".equals(locale) ? "en" : "sq");
        customer.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        return customer;
    }

    @Transactional
    public Customer setConsent(Customer customer, boolean optIn) {
        if (optIn) {
            customer.optIn(clock.instant());
        } else {
            customer.optOut(clock.instant());
        }
        return customer;
    }
}
