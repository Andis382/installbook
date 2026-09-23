package io.github.andis382.installbook.reminders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andis382.installbook.reminders.ReminderService.RunResult;
import io.github.andis382.installbook.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

class ReminderJobTest extends IntegrationTest {

    @Autowired
    private ReminderService reminders;

    /** Install date that puts the first service exactly {@code days} after today (12-month interval). */
    private static LocalDate installedSoThatDueIn(int days) {
        return today().plusDays(days).minusMonths(12);
    }

    private long countMessages(String template) {
        return jdbc.queryForObject("select count(*) from outbound_messages where template_key = ?", Long.class, template);
    }

    @Test
    void onlyUnitsInsideTheLeadWindowAreReminded() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        LocalDate edge = installedSoThatDueIn(30);
        recordInstall(owner, "0692281044", true, edge, "INSIDE");
        recordInstall(owner, "0671112233", true, installedSoThatDueIn(31), "OUTSIDE");
        assertThat(edge.plusMonths(12)).isEqualTo(today().plusDays(30));

        RunResult result = reminders.run(organizationId(owner), today());

        assertThat(result.sent()).isEqualTo(1);
        String to = jdbc.queryForObject("select recipient from outbound_messages where template_key = 'service_due'", String.class);
        assertThat(to).isEqualTo("355692281044");
    }

    @Test
    void runningTheJobAgainTheSameDaySendsNothingNew() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "0692281044", true, installedSoThatDueIn(12), "A1");
        recordInstall(owner, "0671112233", true, installedSoThatDueIn(-20), "B2");
        Long org = organizationId(owner);

        RunResult first = reminders.run(org, today());
        RunResult second = reminders.run(org, today());
        RunResult nextDay = reminders.run(org, today().plusDays(1));

        assertThat(first.sent()).isEqualTo(2);
        assertThat(second.sent()).isZero();
        assertThat(second.skipped()).isEqualTo(2);
        assertThat(nextDay.sent()).isZero();
        assertThat(countMessages("service_due") + countMessages("service_overdue")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from reminders", Long.class)).isEqualTo(2);
    }

    @Test
    void anOverdueUnitGetsTheCalmOverdueWording() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "0692281044", true, today().minusMonths(14), "A1");

        reminders.run(organizationId(owner), today());

        String body = jdbc.queryForObject("select body from outbound_messages where template_key = 'service_overdue'", String.class);
        assertThat(body).contains("was due for its regular service", "There is no rush", "Reply STOP");
    }

    @Test
    void withoutConsentNoMessageGoesOutUntilTheCustomerOptsIn() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        JsonNode unit = recordInstall(owner, "0692281044", false, installedSoThatDueIn(10), "A1").path("unit");
        Long org = organizationId(owner);

        RunResult first = reminders.run(org, today());
        assertThat(first.noConsent()).isEqualTo(1);
        assertThat(first.sent()).isZero();
        assertThat(countMessages("service_due")).isZero();
        assertThat(reminders.run(org, today()).noConsent()).isZero();

        mvc.perform(put("/api/customers/" + unit.path("customer").path("id").asLong() + "/consent").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"optIn\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.whatsappOptIn").value(true));

        assertThat(reminders.run(org, today()).sent()).isEqualTo(1);
        assertThat(countMessages("service_due")).isEqualTo(1);
    }

    @Test
    void aCustomerWhoAlreadyAskedForAVisitIsNotReminded() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        JsonNode unit = recordInstall(owner, "0692281044", true, installedSoThatDueIn(10), "A1").path("unit");
        mvc.perform(post("/api/public/cards/" + cardToken(unit) + "/bookings").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"preferredDate\":\"%s\",\"preferredPeriod\":\"MORNING\"}".formatted(today().plusDays(3))))
            .andExpect(status().isOk());

        RunResult result = reminders.run(organizationId(owner), today());

        assertThat(result.sent()).isZero();
        assertThat(countMessages("service_due")).isZero();
    }

    @Test
    void theOwnerCanRunItFromTheDueList() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "0692281044", true, installedSoThatDueIn(5), "A1");
        recordInstall(owner, "0671112233", false, installedSoThatDueIn(6), "B2");

        mvc.perform(post("/api/reminders/run").cookie(owner).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sent").value(1))
            .andExpect(jsonPath("$.noConsent").value(1));
        mvc.perform(post("/api/reminders/run").cookie(owner).with(csrf()))
            .andExpect(jsonPath("$.sent").value(0));
    }

    @Test
    void theWeeklyDigestGoesToTheBusinessPhone() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "0692281044", true, today().minusMonths(13), "A1");

        mvc.perform(post("/api/reminders/digest").cookie(owner).with(csrf())).andExpect(status().isConflict());

        mvc.perform(put("/api/organization").cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Termo Hoxha\",\"phone\":\"069 420 1177\",\"locale\":\"en\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/reminders/digest").cookie(owner).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipient").value("355694201177"))
            .andExpect(jsonPath("$.templateKey").value("installer_digest"))
            .andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString("1 overdue")));
    }
}
