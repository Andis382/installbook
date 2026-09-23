package io.github.andis382.installbook.visits;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Something done to a unit after it was fitted: a service, a repair, an inspection, a claim. */
@Entity
@Table(name = "service_visits")
public class ServiceVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "visited_on", nullable = false)
    private LocalDate visitedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceKind kind;

    @Column(name = "price_cents")
    private Integer priceCents;

    @Column(columnDefinition = "text")
    private String parts;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "performed_by")
    private Long performedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ServiceVisit() {}

    public ServiceVisit(Long organizationId, Long unitId, LocalDate visitedOn, ServiceKind kind, Integer priceCents,
                        String parts, String notes, Long performedBy) {
        this.organizationId = organizationId;
        this.unitId = unitId;
        this.visitedOn = visitedOn;
        this.kind = kind;
        this.priceCents = priceCents;
        this.parts = parts;
        this.notes = notes;
        this.performedBy = performedBy;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Long getUnitId() { return unitId; }
    public LocalDate getVisitedOn() { return visitedOn; }
    public ServiceKind getKind() { return kind; }
    public Integer getPriceCents() { return priceCents; }
    public String getParts() { return parts; }
    public String getNotes() { return notes; }
    public Long getPerformedBy() { return performedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
