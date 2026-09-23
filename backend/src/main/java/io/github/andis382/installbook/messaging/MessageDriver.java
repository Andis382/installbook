package io.github.andis382.installbook.messaging;

/** Delivers one message. Implementations must not throw; they report failure in the result. */
public interface MessageDriver {

    String name();

    DeliveryResult deliver(OutboundMessage message);

    record DeliveryResult(boolean ok, String providerMessageId, OutboundMessage.Status status, String error) {

        public static DeliveryResult sent(String providerMessageId) {
            return new DeliveryResult(true, providerMessageId, OutboundMessage.Status.SENT, null);
        }

        public static DeliveryResult simulated() {
            return new DeliveryResult(true, null, OutboundMessage.Status.SIMULATED, null);
        }

        public static DeliveryResult failed(String error) {
            return new DeliveryResult(false, null, OutboundMessage.Status.FAILED, error);
        }
    }
}
