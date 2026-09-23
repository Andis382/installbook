package io.github.andis382.installbook.bookings;

import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.units.Unit;
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
import java.util.EnumSet;
import java.util.Set;

/**
 * A customer asking for a service visit, from the warranty card or by answering a reminder.
 * NEW until the installer calls and fixes a time (SCHEDULED), then DONE with a visit, or DECLINED.
 */
@Entity
@Table(name = "booking_requests")
public class BookingRequest {

    public enum Status {
        NEW, SCHEDULED, DONE, DECLINED;

        public boolean isOpen() {
            return this == NEW || this == SCHEDULED;
        }
    }

    public enum Source { CARD, WHATSAPP }

    public enum Period { MORNING, AFTERNOON, ANY }

    private static final Set<Status> SCHEDULABLE = EnumSet.of(Status.NEW, Status.SCHEDULED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "reminder_id")
    private Long reminderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "preferred_date")
    private LocalDate preferredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_period")
    private Period preferredPeriod;

    @Column(columnDefinition = "text")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "visit_id")
    private Long visitId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected BookingRequest() {}

    public BookingRequest(Long organizationId, Unit unit, Source source, LocalDate preferredDate, Period preferredPeriod,
                          String note, Instant createdAt) {
        this.organizationId = organizationId;
        this.unit = unit;
        this.source = source;
        this.preferredDate = preferredDate;
        this.preferredPeriod = preferredPeriod;
        this.note = note;
        this.createdAt = createdAt;
    }

    public void schedule(Instant at, Instant now) {
        requireStatus(SCHEDULABLE);
        this.status = Status.SCHEDULED;
        this.scheduledAt = at;
        this.decidedAt = now;
    }

    public void done(Long visitId, Instant now) {
        requireStatus(SCHEDULABLE);
        this.status = Status.DONE;
        this.visitId = visitId;
        this.decidedAt = now;
    }

    public void decline(Instant now) {
        requireStatus(SCHEDULABLE);
        this.status = Status.DECLINED;
        this.decidedAt = now;
    }

    private void requireStatus(Set<Status> allowed) {
        if (!allowed.contains(status)) {
            throw ApiException.conflict("booking.already_closed");
        }
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Unit getUnit() { return unit; }
    public Long getReminderId() { return reminderId; }
    public void setReminderId(Long reminderId) { this.reminderId = reminderId; }
    public Source getSource() { return source; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public Period getPreferredPeriod() { return preferredPeriod; }
    public String getNote() { return note; }
    public Status getStatus() { return status; }
    public Instant getScheduledAt() { return scheduledAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public Long getVisitId() { return visitId; }
    public Instant getCreatedAt() { return createdAt; }
}
