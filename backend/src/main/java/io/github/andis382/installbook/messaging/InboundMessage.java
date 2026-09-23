package io.github.andis382.installbook.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A message a customer sent to the platform number (a reply, a button tap, a photo). */
@Entity
@Table(name = "inbound_messages")
public class InboundMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "from_phone", nullable = false)
    private String fromPhone;

    @Column(columnDefinition = "text")
    private String body;

    /** text, button, interactive, image, audio, document... */
    @Column(nullable = false)
    private String kind = "text";

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "provider_message_id", unique = true)
    private String providerMessageId;

    @Column(nullable = false)
    private boolean handled;

    @Column(name = "handled_by")
    private String handledBy;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected InboundMessage() {}

    public InboundMessage(String fromPhone, String body, String kind, String mediaId, String mediaType, String providerMessageId) {
        this.fromPhone = fromPhone;
        this.body = body;
        this.kind = kind;
        this.mediaId = mediaId;
        this.mediaType = mediaType;
        this.providerMessageId = providerMessageId;
    }

    public void markHandled(String handler, Long organizationId, String relatedType, Long relatedId) {
        this.handled = true;
        this.handledBy = handler;
        this.organizationId = organizationId;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
    }

    /** Lower-cased, trimmed text for keyword matching ("1", "po", "ready?"). */
    public String normalizedBody() {
        return body == null ? "" : body.trim().toLowerCase();
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getFromPhone() { return fromPhone; }
    public String getBody() { return body; }
    public String getKind() { return kind; }
    public String getMediaId() { return mediaId; }
    public String getMediaType() { return mediaType; }
    public String getProviderMessageId() { return providerMessageId; }
    public boolean isHandled() { return handled; }
    public String getHandledBy() { return handledBy; }
    public String getRelatedType() { return relatedType; }
    public Long getRelatedId() { return relatedId; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
