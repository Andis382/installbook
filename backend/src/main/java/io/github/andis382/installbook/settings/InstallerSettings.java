package io.github.andis382.installbook.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** How one installer works: the defaults a new install starts from and when reminders go out. */
@Entity
@Table(name = "installer_settings")
public class InstallerSettings {

    public static final int DEFAULT_WARRANTY_MONTHS = 24;
    public static final int DEFAULT_SERVICE_INTERVAL_MONTHS = 12;
    public static final int DEFAULT_LEAD_DAYS = 30;
    public static final int DEFAULT_SERVICE_PRICE_CENTS = 5000;

    @Id
    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "default_warranty_months", nullable = false)
    private int defaultWarrantyMonths = DEFAULT_WARRANTY_MONTHS;

    @Column(name = "default_service_interval_months", nullable = false)
    private int defaultServiceIntervalMonths = DEFAULT_SERVICE_INTERVAL_MONTHS;

    @Column(name = "reminder_lead_days", nullable = false)
    private int reminderLeadDays = DEFAULT_LEAD_DAYS;

    @Column(name = "typical_service_price_cents", nullable = false)
    private int typicalServicePriceCents = DEFAULT_SERVICE_PRICE_CENTS;

    @Column(name = "public_phone")
    private String publicPhone;

    @Column(name = "last_reminder_run_on")
    private LocalDate lastReminderRunOn;

    @Column(name = "last_digest_on")
    private LocalDate lastDigestOn;

    protected InstallerSettings() {}

    public InstallerSettings(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getOrganizationId() { return organizationId; }
    public int getDefaultWarrantyMonths() { return defaultWarrantyMonths; }
    public void setDefaultWarrantyMonths(int defaultWarrantyMonths) { this.defaultWarrantyMonths = defaultWarrantyMonths; }
    public int getDefaultServiceIntervalMonths() { return defaultServiceIntervalMonths; }
    public void setDefaultServiceIntervalMonths(int months) { this.defaultServiceIntervalMonths = months; }
    public int getReminderLeadDays() { return reminderLeadDays; }
    public void setReminderLeadDays(int reminderLeadDays) { this.reminderLeadDays = reminderLeadDays; }
    public int getTypicalServicePriceCents() { return typicalServicePriceCents; }
    public void setTypicalServicePriceCents(int cents) { this.typicalServicePriceCents = cents; }
    public String getPublicPhone() { return publicPhone; }
    public void setPublicPhone(String publicPhone) { this.publicPhone = publicPhone; }
    public LocalDate getLastReminderRunOn() { return lastReminderRunOn; }
    public void setLastReminderRunOn(LocalDate lastReminderRunOn) { this.lastReminderRunOn = lastReminderRunOn; }
    public LocalDate getLastDigestOn() { return lastDigestOn; }
    public void setLastDigestOn(LocalDate lastDigestOn) { this.lastDigestOn = lastDigestOn; }
}
