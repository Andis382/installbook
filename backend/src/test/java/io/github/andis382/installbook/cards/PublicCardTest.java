package io.github.andis382.installbook.cards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andis382.installbook.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** The customer's warranty card: reachable with the link alone, and showing nothing else. */
class PublicCardTest extends IntegrationTest {

    private String token;

    @BeforeEach
    void anInstallWithAService() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        JsonNode unit = recordInstall(owner, "0692281044", true, today().minusMonths(13), "21223300100N7").path("unit");
        mvc.perform(post("/api/units/" + unit.path("id").asLong() + "/visits").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visitedOn\":\"%s\",\"kind\":\"ANNUAL_SERVICE\",\"priceCents\":5500,\"parts\":\"Filter cleaned\",\"notes\":\"Customer was rude\"}"
                    .formatted(today().minusMonths(1))))
            .andExpect(status().isCreated());
        Cookie other = registerOwner("klodi@example.com", "Klima Durrës");
        recordInstall(other, "0671112233", true, today(), "OTHER-ORG-SERIAL");
        token = cardToken(unit);
    }

    @Test
    void theCardShowsTheUnitAndItsServiceHistory() throws Exception {
        mvc.perform(get("/api/public/cards/" + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.business.name").value("Termo Hoxha"))
            .andExpect(jsonPath("$.unit.brand").value("Vaillant"))
            .andExpect(jsonPath("$.unit.serialNumber").value("21223300100N7"))
            .andExpect(jsonPath("$.unit.warrantyActive").value(true))
            .andExpect(jsonPath("$.unit.lastServiceOn").value(today().minusMonths(1).toString()))
            .andExpect(jsonPath("$.history.length()").value(1))
            .andExpect(jsonPath("$.history[0].parts").value("Filter cleaned"))
            .andExpect(jsonPath("$.locale").value("en"))
            .andExpect(jsonPath("$.cardNumber").value(org.hamcrest.Matchers.startsWith("IB-")));
    }

    @Test
    void theCardLeaksNothingFromTheRegister() throws Exception {
        String body = mvc.perform(get("/api/public/cards/" + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(body)
            .doesNotContain("organizationId", "\"id\"", "customerId", "355692281044")
            .doesNotContain("OTHER-ORG-SERIAL", "Klima Durrës")
            .doesNotContain("Customer was rude", "5500", token);
    }

    @Test
    void anUnknownTokenFindsNothing() throws Exception {
        mvc.perform(get("/api/public/cards/not-a-real-token")).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/cards/not-a-real-token/certificate.pdf")).andExpect(status().isNotFound());
    }

    @Test
    void bookingFromTheCardIsCreatedOnlyOnce() throws Exception {
        String request = "{\"preferredDate\":\"%s\",\"preferredPeriod\":\"AFTERNOON\",\"note\":\"Ring twice\"}"
            .formatted(today().plusDays(4));
        mvc.perform(post("/api/public/cards/" + token + "/bookings").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(true))
            .andExpect(jsonPath("$.booking.status").value("NEW"))
            .andExpect(jsonPath("$.booking.preferredPeriod").value("AFTERNOON"));
        mvc.perform(post("/api/public/cards/" + token + "/bookings").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(jsonPath("$.created").value(false));

        mvc.perform(get("/api/public/cards/" + token))
            .andExpect(jsonPath("$.openBooking.status").value("NEW"));
        assertThat(jdbc.queryForObject("select count(*) from booking_requests", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from outbound_messages where template_key = 'booking_received'", Long.class))
            .isEqualTo(1);
    }

    @Test
    void thePreferredDayMustBeWithinTheNextThreeMonths() throws Exception {
        mvc.perform(post("/api/public/cards/" + token + "/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredDate\":\"%s\",\"preferredPeriod\":\"MORNING\"}".formatted(today().minusDays(1))))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.preferredDate[0]").exists());
        mvc.perform(post("/api/public/cards/" + token + "/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredDate\":\"%s\",\"preferredPeriod\":\"MORNING\"}".formatted(today().plusMonths(5))))
            .andExpect(status().isUnprocessableContent());
    }

    @Test
    void theCertificateIsAOnePagePdf() throws Exception {
        byte[] pdf = mvc.perform(get("/api/public/cards/" + token + "/certificate.pdf").param("lang", "sq"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1)).contains("/Count 1");
    }
}
