package io.github.andis382.installbook.messaging;

import io.github.andis382.installbook.config.AppProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * WhatsApp Cloud API. Business-initiated messages need an approved template, configured as
 * app.messaging.whatsapp.templates.&lt;key&gt;=&lt;template_name&gt;|param1,param2 ; without a
 * mapping the message goes out as plain text, which WhatsApp only accepts inside the
 * 24-hour window after the customer last wrote.
 */
@Component
public class WhatsAppCloudDriver implements MessageDriver {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudDriver.class);

    private final AppProperties props;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public WhatsAppCloudDriver(AppProperties props, ObjectMapper json) {
        this.props = props;
        this.json = json;
    }

    @Override
    public String name() {
        return "whatsapp";
    }

    public boolean configured() {
        AppProperties.WhatsApp wa = props.getMessaging().getWhatsapp();
        return !wa.getToken().isBlank() && !wa.getPhoneNumberId().isBlank();
    }

    @Override
    public DeliveryResult deliver(OutboundMessage message) {
        if (!configured()) {
            return DeliveryResult.failed("WhatsApp is not configured");
        }
        AppProperties.WhatsApp wa = props.getMessaging().getWhatsapp();
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", message.getRecipient());
            String mapping = wa.getTemplates().get(message.getTemplateKey());
            if (mapping != null && !mapping.isBlank()) {
                payload.put("type", "template");
                payload.put("template", template(mapping, message));
            } else {
                payload.put("type", "text");
                payload.put("text", Map.of("body", message.getBody(), "preview_url", true));
            }
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/" + wa.getApiVersion() + "/" + wa.getPhoneNumberId() + "/messages"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + wa.getToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("WhatsApp send failed: {} {}", response.statusCode(), response.body());
                return DeliveryResult.failed("HTTP " + response.statusCode() + ": " + response.body());
            }
            JsonNode body = json.readTree(response.body());
            String id = body.path("messages").path(0).path("id").asString(null);
            return DeliveryResult.sent(id);
        } catch (Exception e) {
            log.warn("WhatsApp send error", e);
            return DeliveryResult.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Map<String, Object> template(String mapping, OutboundMessage message) {
        String[] parts = mapping.split("\\|", 2);
        Map<String, String> params = message.getParamsJson() == null ? Map.of()
            : json.readValue(message.getParamsJson(), new TypeReference<Map<String, String>>() {});
        List<Map<String, String>> parameters = new ArrayList<>();
        if (parts.length > 1) {
            for (String name : parts[1].split(",")) {
                String key = name.trim();
                String value = "link".equals(key) ? message.getLink() : params.get(key);
                parameters.add(Map.of("type", "text", "text", value == null ? "-" : value));
            }
        }
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", parts[0].trim());
        template.put("language", Map.of("code", message.getLocale()));
        if (!parameters.isEmpty()) {
            template.put("components", List.of(Map.of("type", "body", "parameters", parameters)));
        }
        return template;
    }

    /** Downloads media a customer sent (photo, voice note). Returns bytes and MIME type. */
    public Optional<Media> downloadMedia(String mediaId) {
        if (!configured() || mediaId == null) {
            return Optional.empty();
        }
        AppProperties.WhatsApp wa = props.getMessaging().getWhatsapp();
        try {
            HttpRequest meta = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/" + wa.getApiVersion() + "/" + mediaId))
                .header("Authorization", "Bearer " + wa.getToken())
                .GET().build();
            JsonNode info = json.readTree(http.send(meta, HttpResponse.BodyHandlers.ofString()).body());
            String url = info.path("url").asString(null);
            String mime = info.path("mime_type").asString("application/octet-stream");
            if (url == null) {
                return Optional.empty();
            }
            HttpRequest file = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Authorization", "Bearer " + wa.getToken()).GET().build();
            byte[] bytes = http.send(file, HttpResponse.BodyHandlers.ofByteArray()).body();
            return Optional.of(new Media(bytes, mime));
        } catch (Exception e) {
            log.warn("WhatsApp media download failed", e);
            return Optional.empty();
        }
    }

    public record Media(byte[] bytes, String mimeType) {}
}
