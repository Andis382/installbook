package io.github.andis382.installbook.messaging;

import io.github.andis382.installbook.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Meta's webhook: verification handshake, customer messages and delivery receipts. */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final AppProperties props;
    private final ObjectMapper json;
    private final InboundDispatcher dispatcher;
    private final OutboundMessageRepository outbox;

    public WhatsAppWebhookController(AppProperties props, ObjectMapper json, InboundDispatcher dispatcher,
                                     OutboundMessageRepository outbox) {
        this.props = props;
        this.json = json;
        this.dispatcher = dispatcher;
        this.outbox = outbox;
    }

    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        String expected = props.getMessaging().getWhatsapp().getVerifyToken();
        if ("subscribe".equals(mode) && !expected.isBlank() && expected.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> receive(@RequestBody byte[] raw,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        if (!validSignature(raw, signature)) {
            return ResponseEntity.status(401).build();
        }
        JsonNode root = json.readTree(raw);
        for (JsonNode entry : root.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");
                for (JsonNode m : value.path("messages")) {
                    dispatcher.receive(toInbound(m));
                }
                for (JsonNode s : value.path("statuses")) {
                    applyStatus(s);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    private InboundMessage toInbound(JsonNode m) {
        String type = m.path("type").asString("text");
        String body = null;
        String mediaId = null;
        String mediaType = null;
        switch (type) {
            case "text" -> body = m.path("text").path("body").asString(null);
            case "button" -> body = m.path("button").path("payload").asString(m.path("button").path("text").asString(null));
            case "interactive" -> {
                JsonNode reply = m.path("interactive").path("button_reply");
                if (reply.isMissingNode()) {
                    reply = m.path("interactive").path("list_reply");
                }
                body = reply.path("id").asString(reply.path("title").asString(null));
            }
            case "image", "audio", "document", "video", "sticker" -> {
                JsonNode media = m.path(type);
                mediaId = media.path("id").asString(null);
                mediaType = media.path("mime_type").asString(null);
                body = media.path("caption").asString(null);
            }
            default -> body = null;
        }
        return new InboundMessage(m.path("from").asString(""), body, type, mediaId, mediaType, m.path("id").asString(null));
    }

    private void applyStatus(JsonNode s) {
        String id = s.path("id").asString(null);
        if (id == null) {
            return;
        }
        Instant at = Instant.ofEpochSecond(s.path("timestamp").asLong(Instant.now().getEpochSecond()));
        outbox.findByProviderMessageId(id).ifPresent(msg -> {
            switch (s.path("status").asString("")) {
                case "delivered" -> msg.markDelivered(at);
                case "read" -> msg.markRead(at);
                case "failed" -> msg.markFailed("whatsapp", s.path("errors").path(0).path("title").asString("failed"));
                default -> { }
            }
            outbox.save(msg);
        });
    }

    private boolean validSignature(byte[] raw, String header) {
        String secret = props.getMessaging().getWhatsapp().getAppSecret();
        if (secret.isBlank()) {
            // Without an app secret we cannot verify; accept only in demo mode.
            if (!props.isDemo()) {
                log.warn("Rejecting webhook: WHATSAPP_APP_SECRET is not set");
            }
            return props.isDemo();
        }
        if (header == null || !header.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(raw));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                header.substring(7).getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }
}
