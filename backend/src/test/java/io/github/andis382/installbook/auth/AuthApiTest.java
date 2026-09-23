package io.github.andis382.installbook.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.andis382.installbook.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiTest extends IntegrationTest {

    private static final String REGISTER = """
        {"name":"Arben Hoxha","email":"arben@example.com","password":"secret123","organizationName":"Hoxha Termo","locale":"sq"}
        """;

    @Test
    void registerSignsInAndCreatesTheOrganisation() throws Exception {
        var result = mvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value("arben@example.com"))
            .andExpect(jsonPath("$.user.role").value("OWNER"))
            .andExpect(jsonPath("$.organization.name").value("Hoxha Termo"))
            .andReturn();
        mvc.perform(get("/api/auth/me").cookie(sessionCookie(result)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.name").value("Arben Hoxha"));
    }

    @Test
    void duplicateEmailIsAFieldError() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REGISTER))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REGISTER))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.email[0]").exists());
    }

    @Test
    void validationErrorsFollowTheRequestLanguage() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).header("Accept-Language", "sq")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"email\":\"x\",\"password\":\"1\",\"organizationName\":\"\"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.name[0]").value("Kjo fushë është e detyrueshme."));
    }

    @Test
    void loginWorksAndWrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(REGISTER))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"arben@example.com\",\"password\":\"wrong-pass\"}"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.errors.email[0]").exists());
        var ok = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ARBEN@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isOk())
            .andReturn();
        mvc.perform(get("/api/auth/me").cookie(sessionCookie(ok))).andExpect(status().isOk());
    }

    @Test
    void apiRequiresASessionAndPostsRequireCsrf() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"password\":\"x\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void invitationLetsAColleagueJoin() throws Exception {
        Cookie owner = registerOwner("owner@example.com", "Hoxha Termo");
        String body = mvc.perform(post("/api/team/invitations").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\",\"name\":\"Driton\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*/join/([A-Za-z0-9_-]+)\".*", "$1");

        mvc.perform(get("/api/auth/invitations/" + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.organizationName").value("Hoxha Termo"));

        mvc.perform(post("/api/auth/join").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"name\":\"Driton\",\"email\":\"driton@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.role").value("TECHNICIAN"))
            .andExpect(jsonPath("$.organization.name").value("Hoxha Termo"));

        mvc.perform(get("/api/auth/invitations/" + token)).andExpect(status().isGone());
    }

    @Test
    void membersCannotInvite() throws Exception {
        Cookie owner = registerOwner("owner@example.com", "Hoxha Termo");
        String body = mvc.perform(post("/api/team/invitations").cookie(owner).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}"))
            .andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*/join/([A-Za-z0-9_-]+)\".*", "$1");
        var joined = mvc.perform(post("/api/auth/join").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"name\":\"M\",\"email\":\"m@example.com\",\"password\":\"secret123\"}"))
            .andExpect(status().isCreated()).andReturn();
        mvc.perform(post("/api/team/invitations").cookie(sessionCookie(joined)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"TECHNICIAN\"}"))
            .andExpect(status().isForbidden());
    }
}
