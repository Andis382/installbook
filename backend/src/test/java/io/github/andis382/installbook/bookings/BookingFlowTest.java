package io.github.andis382.installbook.bookings;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andis382.installbook.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

/** NEW -> SCHEDULED -> DONE (a service visit) or DECLINED, as the Bookings screen drives it. */
class BookingFlowTest extends IntegrationTest {

    private Cookie owner;
    private JsonNode unit;
    private long bookingId;

    @BeforeEach
    void aCustomerAskedFromTheirCard() throws Exception {
        owner = registerOwner("arben@example.com", "Termo Hoxha");
        unit = recordInstall(owner, "0692281044", true, today().plusDays(12).minusMonths(12), "A1").path("unit");
        mvc.perform(post("/api/public/cards/" + cardToken(unit) + "/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredDate\":\"%s\",\"preferredPeriod\":\"MORNING\"}".formatted(today().plusDays(3))))
            .andExpect(status().isOk());
        bookingId = getJson(owner, "/api/bookings?status=NEW").path("items").get(0).path("id").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void schedulingTellsTheCustomerTheTime() throws Exception {
        postJson("/api/bookings/" + bookingId + "/schedule",
                "{\"date\":\"%s\",\"time\":\"10:30\",\"notifyCustomer\":true}".formatted(today().plusDays(3)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.booking.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.message.templateKey").value("booking_scheduled"))
            .andExpect(jsonPath("$.message.body").value(org.hamcrest.Matchers.containsString("10:30")));

        mvc.perform(get("/api/bookings").param("status", "SCHEDULED").cookie(owner))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.counts.NEW").value(0));
    }

    @Test
    void aTimeInThePastIsRejected() throws Exception {
        postJson("/api/bookings/" + bookingId + "/schedule",
                "{\"date\":\"%s\",\"time\":\"10:30\",\"notifyCustomer\":true}".formatted(today().minusDays(1)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.date[0]").exists());
    }

    @Test
    void markingItDoneRecordsTheServiceAndMovesTheCycle() throws Exception {
        postJson("/api/bookings/" + bookingId + "/done",
                "{\"visitedOn\":\"%s\",\"kind\":\"ANNUAL_SERVICE\",\"priceCents\":5500,\"parts\":\"Filter cleaned\"}".formatted(today()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.booking.status").value("DONE"))
            .andExpect(jsonPath("$.booking.visitId").isNumber());

        mvc.perform(get("/api/units/" + unit.path("id").asLong()).cookie(owner))
            .andExpect(jsonPath("$.lastServiceOn").value(today().toString()))
            .andExpect(jsonPath("$.nextServiceDue").value(today().plusMonths(12).toString()))
            .andExpect(jsonPath("$.openBooking").doesNotExist());

        postJson("/api/bookings/" + bookingId + "/schedule",
                "{\"date\":\"%s\",\"time\":\"10:30\",\"notifyCustomer\":false}".formatted(today().plusDays(2)))
            .andExpect(status().isConflict());
    }

    @Test
    void decliningCanSendAPoliteNote() throws Exception {
        postJson("/api/bookings/" + bookingId + "/decline", "{\"notifyCustomer\":true,\"reason\":\"away that week\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.booking.status").value("DECLINED"))
            .andExpect(jsonPath("$.message.templateKey").value("booking_declined"))
            .andExpect(jsonPath("$.message.body").value(org.hamcrest.Matchers.containsString("(away that week)")));
    }

    @Test
    void anotherOrganisationCannotTouchTheBooking() throws Exception {
        Cookie other = registerOwner("klodi@example.com", "Klima Durrës");
        mvc.perform(post("/api/bookings/" + bookingId + "/decline").cookie(other).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"notifyCustomer\":false}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/bookings").param("status", "NEW").cookie(other))
            .andExpect(jsonPath("$.items.length()").value(0));
    }
}
