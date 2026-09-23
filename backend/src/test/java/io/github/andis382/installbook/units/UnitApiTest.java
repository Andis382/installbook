package io.github.andis382.installbook.units;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andis382.installbook.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;

class UnitApiTest extends IntegrationTest {

    @Test
    void recordingAnInstallCreatesTheUnitAndSendsTheWarrantyCard() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        LocalDate installed = today().minusDays(3);

        JsonNode result = recordInstall(owner, "069 228 1044", true, installed, "21223300100N7");

        JsonNode unit = result.path("unit");
        assertThat(unit.path("serialNumber").asString()).isEqualTo("21223300100N7");
        assertThat(unit.path("warrantyUntil").asString()).isEqualTo(installed.plusMonths(24).toString());
        assertThat(unit.path("nextServiceDue").asString()).isEqualTo(installed.plusMonths(12).toString());
        assertThat(unit.path("warrantyActive").asBoolean()).isTrue();
        assertThat(unit.path("customer").path("phone").asString()).isEqualTo("355692281044");
        assertThat(unit.path("customer").path("whatsappOptIn").asBoolean()).isTrue();
        assertThat(result.path("cardMessage").path("templateKey").asString()).isEqualTo("warranty_card");
        assertThat(result.path("cardMessage").path("body").asString())
            .contains("Mira", "Vaillant boiler", "21223300100N7", "/c/" + cardToken(unit));
        assertThat(unit.path("timeline").get(0).path("kind").asString()).isEqualTo("MESSAGE");
    }

    @Test
    void withoutConsentNothingIsSentButAShareLinkIsReady() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");

        JsonNode result = recordInstall(owner, "0692281044", false, today(), null);

        assertThat(result.path("cardMessage").isNull()).isTrue();
        assertThat(result.path("unit").path("cardShareUrl").asString()).startsWith("https://wa.me/355692281044?text=");
        mvc.perform(get("/api/messages").cookie(owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theSameSerialCannotBeRegisteredTwice() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "0692281044", true, today(), "21223300100N7");

        String again = """
            {"type":"BOILER","brand":"Vaillant","serialNumber":"21223300100n7 ","customerPhone":"0671112233",
             "customerName":"Arta Leka","whatsappConsent":false,"address":"Rruga e Kavajës 10, Tiranë",
             "installedOn":"%s","warrantyMonths":24,"serviceIntervalMonths":12}
            """.formatted(today());
        mvc.perform(post("/api/units").cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(again))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.serialNumber[0]").exists());
    }

    @Test
    void anInstallDateInTheFutureIsRejected() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        String body = """
            {"type":"AIR_CONDITIONER","brand":"Daikin","customerPhone":"0692281044","customerName":"Mira Kola",
             "whatsappConsent":true,"address":"Tiranë","installedOn":"%s","warrantyMonths":36,"serviceIntervalMonths":12}
            """.formatted(today().plusDays(2));
        mvc.perform(post("/api/units").cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.installedOn[0]").exists());
    }

    @Test
    void aReturningCustomerIsFoundByPhoneAndGetsTheNewUnit() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "+355 69 228 1044", true, today().minusMonths(8), "A1");

        mvc.perform(get("/api/customers/lookup").param("phone", "069 228 1044").cookie(owner))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.found").value(true))
            .andExpect(jsonPath("$.customer.name").value("Mira Kola"))
            .andExpect(jsonPath("$.units.length()").value(1));

        recordInstall(owner, "00355692281044", false, today(), "B2");
        mvc.perform(get("/api/customers").cookie(owner))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].activeUnits").value(2))
            .andExpect(jsonPath("$[0].whatsappOptIn").value(true));
        mvc.perform(get("/api/customers/lookup").param("phone", "0671112233").cookie(owner))
            .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void anAnnualServiceMovesTheNextDueDateButARepairDoesNot() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        long id = recordInstall(owner, "0692281044", true, today().minusMonths(11), "A1").path("unit").path("id").asLong();
        LocalDate originalDue = today().minusMonths(11).plusMonths(12);

        mvc.perform(post("/api/units/" + id + "/visits").cookie(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"visitedOn\":\"%s\",\"kind\":\"REPAIR\",\"priceCents\":4000,\"parts\":\"Pressure sensor\"}".formatted(today())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.nextServiceDue").value(originalDue.toString()));

        JsonNode unit = read(mvc.perform(post("/api/units/" + id + "/visits").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"visitedOn\":\"%s\",\"kind\":\"ANNUAL_SERVICE\",\"priceCents\":5500}".formatted(today())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.lastServiceOn").value(today().toString()))
            .andExpect(jsonPath("$.nextServiceDue").value(today().plusMonths(12).toString()))
            .andReturn());
        long visitsInTimeline = 0;
        for (JsonNode entry : unit.path("timeline")) {
            if ("VISIT".equals(entry.path("kind").asString())) {
                visitsInTimeline++;
            }
        }
        assertThat(visitsInTimeline).isEqualTo(2);
    }

    @Test
    void unitsAreInvisibleToOtherOrganisations() throws Exception {
        Cookie hoxha = registerOwner("arben@example.com", "Termo Hoxha");
        Cookie other = registerOwner("klodi@example.com", "Klima Durrës");
        long id = recordInstall(hoxha, "0692281044", true, today(), "A1").path("unit").path("id").asLong();

        mvc.perform(get("/api/units/" + id).cookie(other)).andExpect(status().isNotFound());
        mvc.perform(put("/api/units/" + id).cookie(other).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"BOILER","brand":"Baxi","address":"x","installedOn":"%s","warrantyMonths":24,"serviceIntervalMonths":12}
                    """.formatted(today())))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/units/" + id + "/visits").cookie(other).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"visitedOn\":\"%s\",\"kind\":\"ANNUAL_SERVICE\"}".formatted(today())))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/units/" + id + "/remove").cookie(other).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/units").cookie(other)).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/customers").cookie(other)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/due").cookie(other)).andExpect(jsonPath("$.groups[*].rows[*]").isEmpty());

        mvc.perform(get("/api/units/" + id).cookie(hoxha)).andExpect(status().isOk());
    }

    @Test
    void theRegisterSearchesSerialsNamesAndPhones() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        recordInstall(owner, "0692281044", true, today().minusMonths(13), "21223300100N7");
        recordInstall(owner, "0671112233", true, today(), "E0045217");

        mvc.perform(get("/api/units").param("q", "n7").cookie(owner)).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/units").param("q", "067 111").cookie(owner)).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/units").param("q", "mira").cookie(owner)).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/units").param("service", "OVERDUE").cookie(owner))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].serviceState").value("OVERDUE"));
        mvc.perform(get("/api/units").cookie(owner))
            .andExpect(jsonPath("$.items[0].serialNumber").value("21223300100N7"));
    }

    @Test
    void thePlatePhotoIsStoredAndManualEntryWorksWithoutAnAiKey() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        MockMultipartFile photo = new MockMultipartFile("file", "plate.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, 1, 2, 3});

        JsonNode plate = read(mvc.perform(multipart("/api/units/plate").file(photo).cookie(owner).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiEnabled").value(false))
            .andExpect(jsonPath("$.reading").doesNotExist())
            .andReturn());
        String photoId = plate.path("photoId").asString();

        mvc.perform(get("/api/files/" + photoId).cookie(owner))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"));
        Cookie other = registerOwner("klodi@example.com", "Klima Durrës");
        mvc.perform(get("/api/files/" + photoId).cookie(other)).andExpect(status().isNotFound());
    }

    @Test
    void aTechnicianRecordsInstallsButCannotChangeInstallerSettings() throws Exception {
        Cookie owner = registerOwner("arben@example.com", "Termo Hoxha");
        String invite = mvc.perform(post("/api/team/invitations").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\",\"name\":\"Ervis\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = invite.replaceAll(".*/join/([A-Za-z0-9_-]+)\".*", "$1");
        Cookie tech = sessionCookie(mvc.perform(post("/api/auth/join").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"name\":\"Ervis Dema\",\"email\":\"ervis@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isCreated()).andReturn());

        recordInstall(tech, "0692281044", true, today(), "A1");
        mvc.perform(put("/api/settings/installer").cookie(tech).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"defaultWarrantyMonths\":36,\"defaultServiceIntervalMonths\":12,\"reminderLeadDays\":30,\"typicalServicePriceCents\":5000}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/reminders/run").cookie(tech).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/team/invitations").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"OWNER\"}"))
            .andExpect(status().isUnprocessableContent());
    }
}
