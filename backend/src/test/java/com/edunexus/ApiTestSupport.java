package com.edunexus;

import com.edunexus.course.CourseRepository;
import com.edunexus.security.JwtService;
import com.edunexus.user.Role;
import com.edunexus.user.User;
import com.edunexus.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Full-context integration tests against the embedded MongoDB. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.jwt.secret=test-secret-0123456789-0123456789-0123456789")
public abstract class ApiTestSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected CourseRepository courses;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        courses.deleteAll();
        users.deleteAll();
    }

    protected User createUser(String email, Role role) {
        User u = new User();
        u.email = email;
        u.firstName = "First";
        u.lastName = "Last";
        u.passwordHash = encoder.encode("secret123");
        u.roles.add(role);
        return users.save(u);
    }

    protected String tokenFor(User u) {
        return jwtService.generateToken(u);
    }

    protected ResultActions getJson(String url, String token) throws Exception {
        var req = get(url);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    protected ResultActions postJson(String url, Object body, String token) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(body == null ? "" : json.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    protected ResultActions putJson(String url, Object body, String token) throws Exception {
        var req = put(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    protected ResultActions deleteReq(String url, String token) throws Exception {
        var req = delete(url);
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    protected JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
