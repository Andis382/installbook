package io.github.andis382.installbook.geo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns the installer's GPS position into a street address through OpenStreetMap Nominatim.
 * Called from the server because Nominatim's usage policy asks for an identifying User-Agent,
 * which browsers do not let a page set. Any failure returns empty: the address field stays
 * editable and the coordinates are kept either way.
 */
@Service
public class ReverseGeocoder {

    private static final Logger log = LoggerFactory.getLogger(ReverseGeocoder.class);
    private static final String USER_AGENT = "InstallBook/1.0 (installer register; https://github.com/Andis382/installbook)";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper json;
    private final String baseUrl;

    public ReverseGeocoder(ObjectMapper json, @Value("${app.geo.nominatim-url:https://nominatim.openstreetmap.org}") String baseUrl) {
        this.json = json;
        this.baseUrl = baseUrl;
    }

    public Optional<String> address(double latitude, double longitude, String language) {
        try {
            URI uri = URI.create(String.format(Locale.ROOT, "%s/reverse?format=jsonv2&zoom=18&addressdetails=1&lat=%.6f&lon=%.6f",
                baseUrl, latitude, longitude));
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(4))
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", language)
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode body = json.readTree(response.body());
            return Optional.ofNullable(format(body.path("address")))
                .or(() -> Optional.ofNullable(body.path("display_name").asString(null)));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("Reverse geocoding failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** "Rruga Myslym Shyri 42, Tiranë": street and number, then the place, the way addresses are written here. */
    static String format(JsonNode address) {
        if (address == null || address.isMissingNode() || address.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        String road = text(address, "road", "pedestrian", "footway", "neighbourhood", "suburb");
        if (road != null) {
            String number = text(address, "house_number");
            parts.add(number == null ? road : road + " " + number);
        }
        String place = text(address, "city", "town", "village", "municipality", "county");
        if (place != null) {
            parts.add(place);
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static String text(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = node.path(field).asString(null);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
