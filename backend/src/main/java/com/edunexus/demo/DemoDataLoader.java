package com.edunexus.demo;

import com.edunexus.course.Course;
import com.edunexus.course.Course.Announcement;
import com.edunexus.course.Course.ContentType;
import com.edunexus.course.Course.CourseModule;
import com.edunexus.course.Course.Discussion;
import com.edunexus.course.Course.ModuleContent;
import com.edunexus.course.CourseRepository;
import com.edunexus.user.Role;
import com.edunexus.user.User;
import com.edunexus.user.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Fictional demo data. Runs ONLY with profile "dev" AND DEMO_DATA=true, and only when the
 * users collection is empty (idempotent). All accounts share a documented demo-only password.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "demo1234";
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final UserRepository users;
    private final CourseRepository courses;
    private final PasswordEncoder encoder;

    public DemoDataLoader(UserRepository users, CourseRepository courses, PasswordEncoder encoder) {
        this.users = users;
        this.courses = courses;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            log.info("Demo data skipped: users collection is not empty");
            return;
        }
        User admin = user("admin@demo.edunexus.test", "Ada", "Admin", Role.ADMIN);
        User instructor = user("instructor@demo.edunexus.test", "Ian", "Instructor", Role.INSTRUCTOR);
        User ta = user("ta@demo.edunexus.test", "Tara", "Assistant", Role.INSTRUCTOR);
        User student = user("student@demo.edunexus.test", "Sam", "Student", Role.STUDENT);
        User student2 = user("student2@demo.edunexus.test", "Sky", "Learner", Role.STUDENT);

        Course web = course("Introduction to Web Development",
                "Learn HTML, CSS and JavaScript fundamentals by building small projects.", instructor);
        web.teachingAssistants.add(ta.id);
        web.modules.add(module("HTML Basics", "Document structure, semantic elements and forms.", 7,
                content("Welcome video", ContentType.VIDEO, "https://example.com/videos/html-intro"),
                content("HTML cheat sheet", ContentType.DOCUMENT, "https://example.com/docs/html-cheatsheet")));
        web.modules.add(module("Styling with CSS", "Selectors, the box model and responsive layouts.", 14,
                content("Layout quiz", ContentType.QUIZ, "Ten questions on flexbox and grid."),
                content("Build a landing page", ContentType.ASSIGNMENT, "Create a responsive landing page.")));
        Announcement welcome = new Announcement();
        welcome.id = id();
        welcome.title = "Welcome to the course";
        welcome.content = "Please review Module 1 before our first session.";
        welcome.authorId = instructor.id;
        welcome.createdAt = Instant.now().minus(2, ChronoUnit.DAYS);
        web.announcements.add(welcome);
        Discussion q = new Discussion();
        q.id = id();
        q.title = "Best resources for practising CSS?";
        q.content = "Share any sites or exercises that helped you.";
        q.authorId = student.id;
        q.createdAt = Instant.now().minus(1, ChronoUnit.DAYS);
        web.discussions.add(q);
        web.enrolledStudents.add(student.id);
        courses.save(web);

        Course data = course("Data Structures Fundamentals",
                "Arrays, linked lists, stacks, queues and trees with practical exercises.", instructor);
        data.modules.add(module("Arrays and Lists", "Memory layout and common operations.", 10,
                content("Lecture notes", ContentType.DOCUMENT, "https://example.com/docs/arrays"),
                content("Reference text", ContentType.REFERENCE_BOOK, "Chapter 2 of the course reader.")));
        courses.save(data);

        Course design = course("UI Design Principles",
                "Typography, colour, spacing and accessibility for modern interfaces.", admin);
        courses.save(design);

        instructor.managedCourses.add(web.id);
        instructor.managedCourses.add(data.id);
        admin.managedCourses.add(design.id);
        student.enrolledCourses.add(web.id);
        users.save(instructor);
        users.save(admin);
        users.save(student);
        users.save(student2);
        log.info("Demo data created: 5 users, 3 courses (demo password documented in README)");
    }

    private User user(String email, String first, String last, Role role) {
        User u = new User();
        u.email = email;
        u.firstName = first;
        u.lastName = last;
        u.passwordHash = encoder.encode(DEMO_PASSWORD);
        u.roles.add(role);
        return users.save(u);
    }

    private static Course course(String title, String description, User owner) {
        Course c = new Course();
        c.id = id();
        c.title = title;
        c.description = description;
        c.adminId = owner.id;
        c.createdAt = c.updatedAt = Instant.now();
        return c;
    }

    private static CourseModule module(String title, String description, int dueInDays, ModuleContent... contents) {
        CourseModule m = new CourseModule();
        m.id = id();
        m.title = title;
        m.description = description;
        m.dueDate = Instant.now().plus(dueInDays, ChronoUnit.DAYS);
        m.contents.addAll(java.util.List.of(contents));
        return m;
    }

    private static ModuleContent content(String title, ContentType type, String body) {
        ModuleContent c = new ModuleContent();
        c.id = id();
        c.title = title;
        c.type = type;
        c.content = body;
        return c;
    }

    private static String id() {
        return new ObjectId().toHexString();
    }
}
