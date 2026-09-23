package io.github.andis382.installbook.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ReverseGeocoderTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void writesStreetNumberAndPlaceTheLocalWay() {
        var address = json.readTree("""
            {"house_number":"42","road":"Rruga Myslym Shyri","suburb":"Njësia Bashkiake 5","city":"Tiranë",
             "county":"Qarku i Tiranës","postcode":"1001","country":"Shqipëria"}
            """);
        assertThat(ReverseGeocoder.format(address)).isEqualTo("Rruga Myslym Shyri 42, Tiranë");
    }

    @Test
    void fallsBackToTheNeighbourhoodAndTownWhenThereIsNoRoad() {
        var address = json.readTree("""
            {"neighbourhood":"Bathore","town":"Kamëz","country":"Shqipëria"}
            """);
        assertThat(ReverseGeocoder.format(address)).isEqualTo("Bathore, Kamëz");
    }

    @Test
    void returnsNothingForAnEmptyAnswer() {
        assertThat(ReverseGeocoder.format(json.readTree("{}"))).isNull();
    }

    @Test
    void anUnreachableServiceIsASilentEmptyResult() {
        ReverseGeocoder geocoder = new ReverseGeocoder(json, "http://127.0.0.1:9");
        assertThat(geocoder.address(41.3275, 19.8187, "sq")).isEmpty();
    }
}
