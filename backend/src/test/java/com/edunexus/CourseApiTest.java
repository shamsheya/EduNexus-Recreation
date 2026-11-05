package com.edunexus;

import com.edunexus.user.Role;
import com.edunexus.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Milestones M4-M5 acceptance criteria, including the role-permission matrix. */
class CourseApiTest extends ApiTestSupport {

    User student, student2, instructor, otherInstructor, ta, admin;
    String tStudent, tStudent2, tInstructor, tOther, tTa, tAdmin;

    @BeforeEach
    void setUpUsers() {
        student = createUser("s@test.dev", Role.STUDENT);
        student2 = createUser("s2@test.dev", Role.STUDENT);
        instructor = createUser("i@test.dev", Role.INSTRUCTOR);
        otherInstructor = createUser("o@test.dev", Role.INSTRUCTOR);
        ta = createUser("ta@test.dev", Role.INSTRUCTOR);
        admin = createUser("a@test.dev", Role.ADMIN);
        tStudent = tokenFor(student);
        tStudent2 = tokenFor(student2);
        tInstructor = tokenFor(instructor);
        tOther = tokenFor(otherInstructor);
        tTa = tokenFor(ta);
        tAdmin = tokenFor(admin);
    }

    private Map<String, Object> courseBody() {
        return Map.of(
                "title", "Intro",
                "description", "Desc",
                "modules", List.of(Map.of(
                        "title", "M1", "description", "D1", "dueDate", "2030-01-01T00:00:00Z",
                        "contents", List.of(Map.of("title", "Video", "type", "video", "content", "https://example.com/v")))));
    }

    private String createCourse(String token) throws Exception {
        return body(postJson("/api/courses", courseBody(), token).andExpect(status().isCreated())).get("id").asText();
    }

    @Test
    void listAndDetailRequireAuthentication() throws Exception {
        String id = createCourse(tInstructor);
        getJson("/api/courses", null).andExpect(status().isUnauthorized());
        getJson("/api/courses/" + id, null).andExpect(status().isUnauthorized());
        getJson("/api/courses", tStudent).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        getJson("/api/courses/does-not-exist", tStudent).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void createSetsOwnerIdsAndLowercaseContentTypes() throws Exception {
        var res = postJson("/api/courses", courseBody(), tInstructor).andExpect(status().isCreated());
        JsonNode c = body(res);
        assertThat(c.get("adminId").asText()).isEqualTo(instructor.id);
        JsonNode module = c.get("modules").get(0);
        assertThat(module.get("id").asText()).isNotBlank();
        assertThat(module.get("contents").get(0).get("id").asText()).isNotBlank();
        assertThat(module.get("contents").get(0).get("type").asText()).isEqualTo("video");
        assertThat(c.get("enrolledStudents").isArray()).isTrue();
        assertThat(users.findById(instructor.id).orElseThrow().managedCourses).containsExactly(c.get("id").asText());
    }

    @Test
    void createPermissions() throws Exception {
        postJson("/api/courses", courseBody(), tStudent).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        postJson("/api/courses", courseBody(), tAdmin).andExpect(status().isCreated());
        postJson("/api/courses", Map.of("title", ""), tInstructor).andExpect(status().isBadRequest());
    }

    @Test
    void updatePermissionsAndWhitelist() throws Exception {
        String id = createCourse(tInstructor);
        var course = courses.findById(id).orElseThrow();
        course.teachingAssistants.add(ta.id);
        courses.save(course);

        putJson("/api/courses/" + id, Map.of("title", "X"), tStudent).andExpect(status().isForbidden());
        putJson("/api/courses/" + id, Map.of("title", "X"), tOther).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not authorized"));
        putJson("/api/courses/" + id, Map.of("title", "By TA"), tTa).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("By TA"));
        putJson("/api/courses/" + id, Map.of("title", "By admin"), tAdmin).andExpect(status().isOk());
        // Mass-assignment attempt: adminId/enrolledStudents are not in the whitelist and must be ignored.
        putJson("/api/courses/" + id, Map.of("title", "Owner", "adminId", student.id,
                "enrolledStudents", List.of(student.id)), tInstructor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminId").value(instructor.id))
                .andExpect(jsonPath("$.enrolledStudents").isEmpty());
    }

    @Test
    void enrolmentRules() throws Exception {
        String id = createCourse(tInstructor);
        postJson("/api/courses/" + id + "/enroll", null, tInstructor).andExpect(status().isForbidden());
        postJson("/api/courses/" + id + "/enroll", null, tStudent).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.enrolledStudents[0]").value(student.id));
        postJson("/api/courses/" + id + "/enroll", null, tStudent).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Already enrolled in this course"));
        assertThat(users.findById(student.id).orElseThrow().enrolledCourses).containsExactly(id);
        postJson("/api/courses/missing/enroll", null, tStudent).andExpect(status().isNotFound());
    }

    @Test
    void deleteRulesAndReferenceCleanup() throws Exception {
        String id = createCourse(tInstructor);
        postJson("/api/courses/" + id + "/enroll", null, tStudent).andExpect(status().isOk());
        deleteReq("/api/courses/" + id, tStudent).andExpect(status().isForbidden());
        deleteReq("/api/courses/" + id, tOther).andExpect(status().isForbidden());
        deleteReq("/api/courses/" + id, tInstructor).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Course deleted successfully"));
        assertThat(courses.existsById(id)).isFalse();
        assertThat(users.findById(instructor.id).orElseThrow().managedCourses).isEmpty();
        assertThat(users.findById(student.id).orElseThrow().enrolledCourses).isEmpty();

        String id2 = createCourse(tInstructor);
        deleteReq("/api/courses/" + id2, tAdmin).andExpect(status().isOk());
    }

    @Test
    void announcementsDiscussionsAndReplies() throws Exception {
        String id = createCourse(tInstructor);
        Map<String, Object> ann = Map.of("title", "Hello", "content", "Welcome");
        postJson("/api/courses/" + id + "/announcements", ann, tStudent).andExpect(status().isForbidden());
        postJson("/api/courses/" + id + "/announcements", ann, tInstructor).andExpect(status().isCreated())
                .andExpect(jsonPath("$.announcements[0].title").value("Hello"))
                .andExpect(jsonPath("$.announcements[0].authorId").value(instructor.id))
                .andExpect(jsonPath("$.announcements[0].createdAt").exists());

        Map<String, Object> disc = Map.of("title", "Q", "content", "Question?", "isAnonymous", true);
        postJson("/api/courses/" + id + "/discussions", disc, tStudent).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You must be enrolled in this course"));
        postJson("/api/courses/" + id + "/enroll", null, tStudent).andExpect(status().isOk());
        JsonNode c = body(postJson("/api/courses/" + id + "/discussions", disc, tStudent).andExpect(status().isCreated()));
        JsonNode d = c.get("discussions").get(0);
        assertThat(d.get("isAnonymous").asBoolean()).isTrue();
        String did = d.get("id").asText();

        Map<String, Object> reply = Map.of("content", "Answer");
        postJson("/api/courses/" + id + "/discussions/" + did + "/replies", reply, tStudent2).andExpect(status().isForbidden());
        postJson("/api/courses/" + id + "/discussions/" + did + "/replies", reply, tInstructor).andExpect(status().isCreated())
                .andExpect(jsonPath("$.discussions[0].replies[0].content").value("Answer"));
        postJson("/api/courses/" + id + "/discussions/unknown/replies", reply, tInstructor).andExpect(status().isNotFound());
        postJson("/api/courses/" + id + "/discussions", Map.of("title", ""), tStudent).andExpect(status().isBadRequest());
    }
}
