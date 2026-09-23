package io.github.andis382.installbook.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Development and "no provider yet" mode: the message stays in the outbox, marked simulated. */
@Component
public class LogMessageDriver implements MessageDriver {

    private static final Logger log = LoggerFactory.getLogger(LogMessageDriver.class);

    @Override
    public String name() {
        return "log";
    }

    @Override
    public DeliveryResult deliver(OutboundMessage message) {
        log.info("[outbox] to={} template={} body={}", message.getRecipient(), message.getTemplateKey(),
            message.getBody().replace('\n', ' '));
        return DeliveryResult.simulated();
    }
}
