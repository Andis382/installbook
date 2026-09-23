package io.github.andis382.installbook.messaging;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboundDispatcher {

    private static final Logger log = LoggerFactory.getLogger(InboundDispatcher.class);

    private final List<InboundHandler> handlers;
    private final InboundMessageRepository inbox;
    private final OutboundMessageRepository outbox;

    public InboundDispatcher(List<InboundHandler> handlers, InboundMessageRepository inbox, OutboundMessageRepository outbox) {
        this.handlers = handlers;
        this.inbox = inbox;
        this.outbox = outbox;
    }

    /** Stores the message once (provider ids are unique) and gives it to the first willing handler. */
    @Transactional
    public InboundMessage receive(InboundMessage message) {
        if (message.getProviderMessageId() != null && inbox.existsByProviderMessageId(message.getProviderMessageId())) {
            return message;
        }
        // Default owner: whoever last wrote to this phone. Handlers may re-assign.
        outbox.findTopByRecipientOrderByCreatedAtDesc(message.getFromPhone())
            .ifPresent(last -> message.setOrganizationId(last.getOrganizationId()));
        inbox.save(message);
        for (InboundHandler handler : handlers) {
            try {
                if (handler.handle(message)) {
                    break;
                }
            } catch (RuntimeException e) {
                log.warn("Inbound handler {} failed", handler.getClass().getSimpleName(), e);
            }
        }
        return inbox.save(message);
    }
}
