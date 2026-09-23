package io.github.andis382.installbook.messaging;

/**
 * Reacts to a customer's message. Handlers are tried in @Order; the first one that
 * returns true owns the message (and marks it with {@link InboundMessage#markHandled}).
 */
public interface InboundHandler {

    boolean handle(InboundMessage message);
}
