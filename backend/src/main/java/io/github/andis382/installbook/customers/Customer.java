package io.github.andis382.installbook.customers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The person a unit was fitted for. Found by phone first, because that is what the
 * installer has in hand at the door. WhatsApp consent is explicit and timestamped.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(nullable = false)
    private String name;

    /** International digits without "+", unique per organisation. */
    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, length = 5)
    private String locale = "sq";

    @Column(name = "whatsapp_opt_in", nullable = false)
    private boolean whatsappOptIn;

    @Column(name = "whatsapp_opt_in_at")
    private Instant whatsappOptInAt;

    @Column(name = "whatsapp_opt_out_at")
    private Instant whatsappOptOutAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Customer() {}

    public Customer(Long organizationId, String name, String phone, String locale) {
        this.organizationId = organizationId;
        this.name = name;
        this.phone = phone;
        this.locale = locale;
    }

    /** Records consent once; saying yes again keeps the original moment it was given. */
    public void optIn(Instant at) {
        if (!whatsappOptIn) {
            whatsappOptIn = true;
            whatsappOptInAt = at;
        }
    }

    public void optOut(Instant at) {
        if (whatsappOptIn) {
            whatsappOptIn = false;
            whatsappOptOutAt = at;
        }
    }

    public String firstName() {
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public boolean isWhatsappOptIn() { return whatsappOptIn; }
    public Instant getWhatsappOptInAt() { return whatsappOptInAt; }
    public Instant getWhatsappOptOutAt() { return whatsappOptOutAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
