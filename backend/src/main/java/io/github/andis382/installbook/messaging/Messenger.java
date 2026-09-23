package io.github.andis382.installbook.messaging;

import io.github.andis382.installbook.config.AppProperties;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** The one way the app writes to customers: render, store in the outbox, deliver. */
@Service
public class Messenger {

    private final OutboundMessageRepository outbox;
    private final TemplateRenderer renderer;
    private final LogMessageDriver logDriver;
    private final WhatsAppCloudDriver whatsApp;
    private final AppProperties props;
    private final ObjectMapper json;
    private final Clock clock;

    public Messenger(OutboundMessageRepository outbox, TemplateRenderer renderer, LogMessageDriver logDriver,
                     WhatsAppCloudDriver whatsApp, AppProperties props, ObjectMapper json, Clock clock) {
        this.outbox = outbox;
        this.renderer = renderer;
        this.logDriver = logDriver;
        this.whatsApp = whatsApp;
        this.props = props;
        this.json = json;
        this.clock = clock;
    }

    /** What to send. {@code params} fill the template's {placeholders}; {link} is always available. */
    public record Outgoing(Long organizationId, String to, String toName, String templateKey, String locale,
                           Map<String, String> params, String link, String relatedType, Long relatedId) {}

    @Transactional
    public OutboundMessage send(Outgoing o) {
        Map<String, String> params = new LinkedHashMap<>(o.params() == null ? Map.of() : o.params());
        if (o.link() != null) {
            params.putIfAbsent("link", o.link());
        }
        String locale = o.locale() == null ? props.getDefaultLocale() : o.locale();
        String body = renderer.render(o.templateKey(), locale, params);
        if (o.link() != null && !body.contains(o.link())) {
            body = body + "\n" + o.link();
        }
        OutboundMessage message = new OutboundMessage(o.organizationId(), OutboundMessage.Channel.WHATSAPP, o.to(), o.toName(),
            o.templateKey(), locale, body, o.link(), json.writeValueAsString(params), o.relatedType(), o.relatedId());
        outbox.save(message);
        deliver(message);
        return message;
    }

    /** Sends (again) through the configured driver. Used on first send and on "retry". */
    @Transactional
    public void deliver(OutboundMessage message) {
        MessageDriver driver = "whatsapp".equalsIgnoreCase(props.getMessaging().getDriver()) && whatsApp.configured()
            ? whatsApp : logDriver;
        MessageDriver.DeliveryResult result = driver.deliver(message);
        if (result.ok()) {
            message.markSent(driver.name(), result.providerMessageId(), result.status(), clock.instant());
        } else {
            message.markFailed(driver.name(), result.error());
        }
        outbox.save(message);
    }

    /** Click-to-chat link so the owner can send any message from their own WhatsApp. */
    public static String waMeUrl(OutboundMessage m) {
        return "https://wa.me/" + m.getRecipient() + "?text=" + URLEncoder.encode(m.getBody(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
