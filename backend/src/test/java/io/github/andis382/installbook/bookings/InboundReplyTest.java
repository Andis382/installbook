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

/** Customers answering a reminder on WhatsApp, through the demo simulator endpoint. */
class InboundReplyTest extends IntegrationTest {

    private Cookie owner;
    private JsonNode unit;

    @BeforeEach
    void aReminderWentOut() throws Exception {
        owner = registerOwner("arben@example.com", "Termo Hoxha");
        unit = recordInstall(owner, "0692281044", true, today().plusDays(10).minusMonths(12), "A1").path("unit");
        mvc.perform(post("/api/reminders/run").cookie(owner).with(csrf()))
            .andExpect(jsonPath("$.sent").value(1));
    }

    private org.springframework.test.web.servlet.ResultActions reply(String text) throws Exception {
        return mvc.perform(post("/api/dev/inbound").cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"from\":\"+355 69 228 1044\",\"body\":\"" + text + "\"}"));
    }

    @Test
    void answeringOneTurnsTheReminderIntoABookingRequest() throws Exception {
        reply("1")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inbound.handled").value(true))
            .andExpect(jsonPath("$.inbound.relatedId").value(unit.path("id").asLong()))
            .andExpect(jsonPath("$.replies[0].templateKey").value("booking_received"));

        mvc.perform(get("/api/bookings").param("status", "NEW").cookie(owner))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].source").value("WHATSAPP"))
            .andExpect(jsonPath("$.items[0].fromReminder").value(true))
            .andExpect(jsonPath("$.counts.NEW").value(1));
        mvc.perform(get("/api/dashboard").cookie(owner))
            .andExpect(jsonPath("$.conversion.sent").value(1))
            .andExpect(jsonPath("$.conversion.booked").value(1))
            .andExpect(jsonPath("$.newBookings.length()").value(1));
    }

    @Test
    void answeringTwiceDoesNotCreateASecondRequest() throws Exception {
        reply("Po").andExpect(jsonPath("$.inbound.handled").value(true));
        reply("po!").andExpect(jsonPath("$.replies[0].templateKey").value("booking_received"));

        mvc.perform(get("/api/bookings").param("status", "NEW").cookie(owner))
            .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void stopWithdrawsConsentAndConfirmsIt() throws Exception {
        reply("NDALO")
            .andExpect(jsonPath("$.inbound.handled").value(true))
            .andExpect(jsonPath("$.replies[0].templateKey").value("opt_out_confirmed"));

        mvc.perform(get("/api/customers/" + unit.path("customer").path("id").asLong()).cookie(owner))
            .andExpect(jsonPath("$.whatsappOptIn").value(false))
            .andExpect(jsonPath("$.whatsappOptOutAt").exists());
    }

    @Test
    void anythingElseIsLeftForAPersonToRead() throws Exception {
        reply("A mund të vini të shtunën?")
            .andExpect(jsonPath("$.inbound.handled").value(false))
            .andExpect(jsonPath("$.replies.length()").value(0));

        mvc.perform(get("/api/bookings").param("status", "NEW").cookie(owner))
            .andExpect(jsonPath("$.items.length()").value(0));
    }
}
