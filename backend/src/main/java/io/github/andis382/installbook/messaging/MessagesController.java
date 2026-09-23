package io.github.andis382.installbook.messaging;

import io.github.andis382.installbook.auth.CurrentUser;
import io.github.andis382.installbook.common.ApiException;
import io.github.andis382.installbook.common.Phones;
import io.github.andis382.installbook.config.AppProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessagesController {

    private final OutboundMessageRepository outbox;
    private final InboundMessageRepository inbox;
    private final InboundDispatcher dispatcher;
    private final Messenger messenger;
    private final CurrentUser currentUser;
    private final AppProperties props;
    private final Clock clock;

    public MessagesController(OutboundMessageRepository outbox, InboundMessageRepository inbox, InboundDispatcher dispatcher,
                              Messenger messenger, CurrentUser currentUser, AppProperties props, Clock clock) {
        this.outbox = outbox;
        this.inbox = inbox;
        this.dispatcher = dispatcher;
        this.messenger = messenger;
        this.currentUser = currentUser;
        this.props = props;
        this.clock = clock;
    }

    public record MessageView(Long id, String channel, String recipient, String recipientName, String templateKey,
                              String body, String link, String status, String error, String relatedType, Long relatedId,
                              Instant createdAt, Instant sentAt, Instant deliveredAt, Instant readAt, String waMeUrl) {
        public static MessageView of(OutboundMessage m) {
            return new MessageView(m.getId(), m.getChannel().name(), m.getRecipient(), m.getRecipientName(), m.getTemplateKey(),
                m.getBody(), m.getLink(), m.getStatus().name(), m.getError(), m.getRelatedType(), m.getRelatedId(),
                m.getCreatedAt(), m.getSentAt(), m.getDeliveredAt(), m.getReadAt(), Messenger.waMeUrl(m));
        }
    }

    public record InboundView(Long id, String fromPhone, String body, String kind, boolean handled, String handledBy,
                              String relatedType, Long relatedId, Instant receivedAt) {
        public static InboundView of(InboundMessage m) {
            return new InboundView(m.getId(), m.getFromPhone(), m.getBody(), m.getKind(), m.isHandled(), m.getHandledBy(),
                m.getRelatedType(), m.getRelatedId(), m.getReceivedAt());
        }
    }

    public record SimulateRequest(@NotBlank @Size(max = 40) String from, @NotBlank @Size(max = 1000) String body) {}

    public record SimulateResponse(InboundView inbound, List<MessageView> replies) {}

    @GetMapping("/api/messages")
    public List<MessageView> outbox(@RequestParam(defaultValue = "100") int limit) {
        return outbox.findByOrganizationIdOrderByCreatedAtDesc(currentUser.organizationId(), PageRequest.of(0, Math.min(limit, 500)))
            .stream().map(MessageView::of).toList();
    }

    @GetMapping("/api/messages/inbound")
    public List<InboundView> inbox(@RequestParam(defaultValue = "100") int limit) {
        return inbox.findByOrganizationIdOrderByReceivedAtDesc(currentUser.organizationId(), PageRequest.of(0, Math.min(limit, 500)))
            .stream().map(InboundView::of).toList();
    }

    @PostMapping("/api/messages/{id}/retry")
    @Transactional
    public MessageView retry(@PathVariable Long id) {
        OutboundMessage m = outbox.findById(id)
            .filter(x -> currentUser.organizationId().equals(x.getOrganizationId()))
            .orElseThrow(ApiException::notFound);
        messenger.deliver(m);
        return MessageView.of(m);
    }

    /** Demo only: pretend a customer replied on WhatsApp, and show what the app answered. */
    @PostMapping("/api/dev/inbound")
    @Transactional
    public SimulateResponse simulate(@Valid @RequestBody SimulateRequest req) {
        if (!props.isDemo()) {
            throw ApiException.notFound();
        }
        String from = Phones.normalize(req.from(), props.getDefaultCountryCode());
        Instant start = clock.instant();
        InboundMessage message = dispatcher.receive(
            new InboundMessage(from, req.body(), "text", null, null, "sim-" + UUID.randomUUID()));
        List<MessageView> replies = outbox.findByRecipientAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(from, start)
            .stream().map(MessageView::of).toList();
        return new SimulateResponse(InboundView.of(message), replies);
    }
}
