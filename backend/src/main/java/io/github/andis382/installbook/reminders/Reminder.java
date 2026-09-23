package io.github.andis382.installbook.reminders;

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

/**
 * The service reminder for one unit and one due cycle. The (unit, due date) pair is unique,
 * which is what makes the daily run safe to repeat, and the outcome is what makes
 * "reminders that turned into bookings" countable.
 */
@Entity
@Table(name = "reminders")
public class Reminder {

    public enum Outcome {
        /** Went out on WhatsApp; no answer yet. */
        SENT,
        /** The customer has not agreed to WhatsApp messages: the installer calls instead. */
        NO_CONSENT,
        /** The provider refused the message. */
        FAILED,
        /** The customer asked for a booking after this reminder. */
        BOOKED,
        /** The unit was serviced this cycle without a booking request through the app. */
        SERVICED
    }

    public enum Trigger { AUTO, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "due_on", nullable = false)
    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_kind", nullable = false)
    private Trigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Reminder() {}

    public Reminder(Long organizationId, Long unitId, LocalDate dueOn, Trigger trigger, Instant createdAt) {
        this.organizationId = organizationId;
        this.unitId = unitId;
        this.dueOn = dueOn;
        this.trigger = trigger;
        this.createdAt = createdAt;
        this.outcome = Outcome.NO_CONSENT;
    }

    public void delivered(Long messageId, Instant at) {
        this.messageId = messageId;
        this.sentAt = at;
        if (outcome != Outcome.BOOKED) {
            this.outcome = Outcome.SENT;
        }
    }

    public void failed(Long messageId) {
        this.messageId = messageId;
        this.outcome = Outcome.FAILED;
    }

    public void noConsent() {
        this.outcome = Outcome.NO_CONSENT;
    }

    public void booked() {
        this.outcome = Outcome.BOOKED;
    }

    public void serviced() {
        if (outcome != Outcome.BOOKED) {
            this.outcome = Outcome.SERVICED;
        }
    }

    public boolean wentOut() {
        return sentAt != null;
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Long getUnitId() { return unitId; }
    public LocalDate getDueOn() { return dueOn; }
    public Trigger getTrigger() { return trigger; }
    public Outcome getOutcome() { return outcome; }
    public Instant getSentAt() { return sentAt; }
    public Long getMessageId() { return messageId; }
    public Instant getCreatedAt() { return createdAt; }
}
