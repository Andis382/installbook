package io.github.andis382.installbook.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Full application against a real PostgreSQL database (see application-test.yml).
 * Every test starts from empty tables. Sessions are Spring Session (JDBC), so a signed-in
 * client is represented by its SESSION cookie.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper json;

    @BeforeEach
    void cleanDatabase() {
        List<String> tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
            String.class);
        if (!tables.isEmpty()) {
            jdbc.execute("TRUNCATE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }

    /** Registers a new owner + organisation and returns the session cookie. */
    protected Cookie registerOwner(String email, String organizationName) throws Exception {
        String body = """
            {"name":"Owner","email":"%s","password":"secret123","organizationName":"%s","locale":"en"}
            """.formatted(email, organizationName);
        MvcResult result = mvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("register failed: " + result.getResponse().getContentAsString());
        }
        return sessionCookie(result);
    }

    protected static Cookie sessionCookie(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie("SESSION");
        if (cookie == null) {
            throw new IllegalStateException("No SESSION cookie in response");
        }
        return cookie;
    }

    /** The installer's calendar day (organisations are created in Europe/Tirane). */
    protected static LocalDate today() {
        return LocalDate.now(ZoneId.of("Europe/Tirane"));
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode getJson(Cookie cookie, String url) throws Exception {
        MvcResult result = mvc.perform(get(url).cookie(cookie)).andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new IllegalStateException(url + " -> " + result.getResponse().getStatus());
        }
        return read(result);
    }

    protected Long organizationId(Cookie cookie) throws Exception {
        return getJson(cookie, "/api/auth/me").path("organization").path("id").asLong();
    }

    /** Records a Vaillant boiler through the API, as the install screen does, and returns the response. */
    protected JsonNode recordInstall(Cookie cookie, String phone, boolean consent, LocalDate installedOn, String serial)
        throws Exception {
        String body = """
            {"type":"BOILER","brand":"Vaillant","model":"ecoTEC plus VU 246/5-5","serialNumber":%s,
             "customerPhone":"%s","customerName":"Mira Kola","customerLocale":"en","whatsappConsent":%s,
             "address":"Rruga Myslym Shyri 42, Tiranë","latitude":41.3239,"longitude":19.8108,
             "installedOn":"%s","warrantyMonths":24,"serviceIntervalMonths":12}
            """.formatted(serial == null ? "null" : "\"" + serial + "\"", phone, consent, installedOn);
        MvcResult result = mvc.perform(post("/api/units").cookie(cookie).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("install failed: " + result.getResponse().getContentAsString());
        }
        return read(result);
    }

    /** The token at the end of a unit's card link. */
    protected static String cardToken(JsonNode unit) {
        String url = unit.path("cardUrl").asString();
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
