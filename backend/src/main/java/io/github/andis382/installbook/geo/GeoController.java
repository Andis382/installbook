package io.github.andis382.installbook.geo;

import io.github.andis382.installbook.common.ApiException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeoController {

    private final ReverseGeocoder geocoder;

    public GeoController(ReverseGeocoder geocoder) {
        this.geocoder = geocoder;
    }

    /** {@code address} is null when the lookup found nothing or failed; the client keeps the coordinates. */
    public record Place(String address) {}

    @GetMapping("/api/geo/reverse")
    public Place reverse(@RequestParam double lat, @RequestParam double lng) {
        if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            throw ApiException.badRequest("error.bad_request");
        }
        return new Place(geocoder.address(lat, lng, LocaleContextHolder.getLocale().getLanguage()).orElse(null));
    }
}
