package com.edunexus;

import com.edunexus.user.Role;
import com.edunexus.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Milestones M1-M3 acceptance criteria (analysis/implementation-plan.md). */
class AuthAndUserApiTest extends ApiTestSupport {

    private static Map<String, Object> registration(String email, String role) {
        return Map.of("firstName", "Jane", "lastName", "Doe", "email", email, "password", "secret123", "role", role);
    }

    @Test
    void healthIsPublic() throws Exception {
        getJson("/api/health", null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void unknownRouteReturnsJson404() throws Exception {
        User u = createUser("a@test.dev", Role.STUDENT);
        getJson("/api/nope", tokenFor(u)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void registerReturns201WithFrontendUserShape() throws Exception {
        var res = postJson("/api/auth/register", registration("Jane@Test.dev", "student"), null)
                .andExpect(status().isCreated());
        JsonNode b = body(res);
        assertThat(b.get("token").asText()).isNotBlank();
        JsonNode user = b.get("user");
        assertThat(user.get("id").asText()).isNotBlank();
        assertThat(user.get("email").asText()).isEqualTo("jane@test.dev");
        assertThat(user.get("roles").get(0).asText()).isEqualTo("student");
        assertThat(user.has("enrolledCourses")).isTrue();
        assertThat(user.has("managedCourses")).isTrue();
        assertThat(b.toString()).doesNotContain("password");
    }

    @Test
    void registerAsInstructorIsAllowed() throws Exception {
        postJson("/api/auth/register", registration("i@test.dev", "instructor"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles[0]").value("instructor"));
    }

    @Test
    void registerAsAdminIsRejected() throws Exception {
        postJson("/api/auth/register", registration("adm@test.dev", "admin"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Admin accounts cannot be self-registered"));
        assertThat(users.count()).isZero();
    }

    @Test
    void registerDuplicateEmailIs400() throws Exception {
        postJson("/api/auth/register", registration("dup@test.dev", "student"), null).andExpect(status().isCreated());
        postJson("/api/auth/register", registration("DUP@test.dev", "student"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void registerValidationErrorsAre400WithFieldMap() throws Exception {
        postJson("/api/auth/register", Map.of("firstName", "", "lastName", "x", "email", "bad", "password", "123"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    void loginSucceedsAndFailsWith400() throws Exception {
        createUser("l@test.dev", Role.STUDENT);
        postJson("/api/auth/login", Map.of("email", "l@test.dev", "password", "secret123"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("l@test.dev"));
        postJson("/api/auth/login", Map.of("email", "l@test.dev", "password", "wrong-pass"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        postJson("/api/auth/login", Map.of("email", "nobody@test.dev", "password", "secret123"), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void meRequiresValidToken() throws Exception {
        User u = createUser("me@test.dev", Role.STUDENT);
        getJson("/api/auth/me", null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").exists());
        getJson("/api/auth/me", "not-a-jwt").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token is not valid"));
        String token = tokenFor(u);
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");
        getJson("/api/auth/me", tampered).andExpect(status().isUnauthorized());
        getJson("/api/auth/me", token).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(u.id));
    }

    @Test
    void tokenOfDeletedUserIsRejected() throws Exception {
        User u = createUser("gone@test.dev", Role.STUDENT);
        String token = tokenFor(u);
        users.deleteById(u.id);
        getJson("/api/users/profile", token).andExpect(status().isUnauthorized());
    }

    @Test
    void profileGetAndUpdate() throws Exception {
        User u = createUser("p@test.dev", Role.STUDENT);
        createUser("taken@test.dev", Role.STUDENT);
        String t = tokenFor(u);
        getJson("/api/users/profile", t).andExpect(status().isOk()).andExpect(jsonPath("$.email").value("p@test.dev"));
        putJson("/api/users/profile", Map.of("firstName", "New", "email", "p2@test.dev"), t)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("Last"))
                .andExpect(jsonPath("$.email").value("p2@test.dev"))
                .andExpect(jsonPath("$.enrolledCourses").isArray());
        putJson("/api/users/profile", Map.of("email", "taken@test.dev"), t)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
        putJson("/api/users/profile", Map.of("email", "not-an-email"), t).andExpect(status().isBadRequest());
    }

    @Test
    void changePassword() throws Exception {
        User u = createUser("pw@test.dev", Role.STUDENT);
        String t = tokenFor(u);
        putJson("/api/users/password", Map.of("currentPassword", "wrong", "newPassword", "newsecret"), t)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        putJson("/api/users/password", Map.of("currentPassword", "secret123", "newPassword", "123"), t)
                .andExpect(status().isBadRequest());
        putJson("/api/users/password", Map.of("currentPassword", "secret123", "newPassword", "newsecret"), t)
                .andExpect(status().isOk());
        postJson("/api/auth/login", Map.of("email", "pw@test.dev", "password", "newsecret"), null)
                .andExpect(status().isOk());
    }
}
