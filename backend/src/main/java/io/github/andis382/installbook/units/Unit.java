package io.github.andis382.installbook.units;

import io.github.andis382.installbook.customers.Customer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One machine at one address: the installer's unit of work. Warranty end and next service
 * date are stored (so lists sort and filter in SQL) and recomputed whenever their inputs change.
 */
@Entity
@Table(name = "units")
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType type;

    @Column(nullable = false)
    private String brand;

    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "plate_photo_id")
    private String platePhotoId;

    @Column(name = "installed_on", nullable = false)
    private LocalDate installedOn;

    @Column(nullable = false)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(name = "warranty_months", nullable = false)
    private int warrantyMonths;

    @Column(name = "warranty_until", nullable = false)
    private LocalDate warrantyUntil;

    @Column(name = "service_interval_months", nullable = false)
    private int serviceIntervalMonths;

    @Column(name = "last_service_on")
    private LocalDate lastServiceOn;

    @Column(name = "next_service_due", nullable = false)
    private LocalDate nextServiceDue;

    @Column(name = "card_token", nullable = false, unique = true)
    private String cardToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitStatus status = UnitStatus.ACTIVE;

    @Column(name = "removed_on")
    private LocalDate removedOn;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "installed_by")
    private Long installedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Unit() {}

    public Unit(Long organizationId, Customer customer, UnitType type, String brand, String cardToken) {
        this.organizationId = organizationId;
        this.customer = customer;
        this.type = type;
        this.brand = brand;
        this.cardToken = cardToken;
    }

    /** Sets the install date and terms, and derives warranty end and next service from them. */
    public void schedule(LocalDate installedOn, int warrantyMonths, int serviceIntervalMonths) {
        this.installedOn = installedOn;
        this.warrantyMonths = warrantyMonths;
        this.serviceIntervalMonths = serviceIntervalMonths;
        if (lastServiceOn != null && lastServiceOn.isBefore(installedOn)) {
            lastServiceOn = null;
        }
        recompute();
    }

    /** A service on this day restarts the cycle (an older, back-filled visit changes nothing). */
    public void serviced(LocalDate on) {
        if (lastServiceOn == null || on.isAfter(lastServiceOn)) {
            lastServiceOn = on;
            recompute();
        }
    }

    public void remove(LocalDate on) {
        status = UnitStatus.REMOVED;
        removedOn = on;
    }

    public void restore() {
        status = UnitStatus.ACTIVE;
        removedOn = null;
    }

    public boolean isActive() {
        return status == UnitStatus.ACTIVE;
    }

    private void recompute() {
        warrantyUntil = ServiceSchedule.warrantyUntil(installedOn, warrantyMonths);
        nextServiceDue = ServiceSchedule.nextServiceDue(installedOn, lastServiceOn, serviceIntervalMonths);
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public UnitType getType() { return type; }
    public void setType(UnitType type) { this.type = type; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getPlatePhotoId() { return platePhotoId; }
    public void setPlatePhotoId(String platePhotoId) { this.platePhotoId = platePhotoId; }
    public LocalDate getInstalledOn() { return installedOn; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public int getWarrantyMonths() { return warrantyMonths; }
    public LocalDate getWarrantyUntil() { return warrantyUntil; }
    public int getServiceIntervalMonths() { return serviceIntervalMonths; }
    public LocalDate getLastServiceOn() { return lastServiceOn; }
    public LocalDate getNextServiceDue() { return nextServiceDue; }
    public String getCardToken() { return cardToken; }
    public UnitStatus getStatus() { return status; }
    public LocalDate getRemovedOn() { return removedOn; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getInstalledBy() { return installedBy; }
    public void setInstalledBy(Long installedBy) { this.installedBy = installedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
