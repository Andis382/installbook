package io.github.andis382.installbook.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Every message the app sends to a customer, kept whether or not a provider is configured.
 * With the "log" driver the outbox is the delivery: the owner sees it and can forward it by hand.
 */
@Entity
@Table(name = "outbound_messages")
public class OutboundMessage {

    public enum Channel { WHATSAPP, SMS, EMAIL }

    public enum Status { QUEUED, SENT, DELIVERED, READ, FAILED, SIMULATED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id")
    private Long organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel = Channel.WHATSAPP;

    @Column(nullable = false)
    private String recipient;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "template_key", nullable = false)
    private String templateKey;

    @Column(nullable = false, length = 5)
    private String locale;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(columnDefinition = "text")
    private String link;

    @Column(name = "params_json", columnDefinition = "text")
    private String paramsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.QUEUED;

    private String provider;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "related_type")
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected OutboundMessage() {}

    public OutboundMessage(Long organizationId, Channel channel, String recipient, String recipientName, String templateKey,
                           String locale, String body, String link, String paramsJson, String relatedType, Long relatedId) {
        this.organizationId = organizationId;
        this.channel = channel;
        this.recipient = recipient;
        this.recipientName = recipientName;
        this.templateKey = templateKey;
        this.locale = locale;
        this.body = body;
        this.link = link;
        this.paramsJson = paramsJson;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
    }

    public void markSent(String provider, String providerMessageId, Status status, Instant at) {
        this.provider = provider;
        this.providerMessageId = providerMessageId;
        this.status = status;
        this.sentAt = at;
    }

    public void markFailed(String provider, String error) {
        this.provider = provider;
        this.status = Status.FAILED;
        this.error = error;
    }

    public void markDelivered(Instant at) {
        if (status != Status.READ) {
            status = Status.DELIVERED;
        }
        deliveredAt = at;
    }

    public void markRead(Instant at) {
        status = Status.READ;
        readAt = at;
    }

    /** Moves a message into the past (demo history): created and sent at that moment. */
    public void backdate(Instant at) {
        createdAt = at;
        if (sentAt != null) {
            sentAt = at;
        }
    }

    public Long getId() { return id; }
    public Long getOrganizationId() { return organizationId; }
    public Channel getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getRecipientName() { return recipientName; }
    public String getTemplateKey() { return templateKey; }
    public String getLocale() { return locale; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public String getParamsJson() { return paramsJson; }
    public Status getStatus() { return status; }
    public String getProvider() { return provider; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getError() { return error; }
    public String getRelatedType() { return relatedType; }
    public Long getRelatedId() { return relatedId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getReadAt() { return readAt; }
}
