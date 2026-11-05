package com.edunexus.course;

import com.edunexus.common.ApiException;
import com.edunexus.course.Course.Announcement;
import com.edunexus.course.Course.CourseModule;
import com.edunexus.course.Course.Discussion;
import com.edunexus.course.Course.DiscussionReply;
import com.edunexus.course.Course.ModuleContent;
import com.edunexus.course.CourseRequests.AnnouncementRequest;
import com.edunexus.course.CourseRequests.CreateCourseRequest;
import com.edunexus.course.CourseRequests.DiscussionRequest;
import com.edunexus.course.CourseRequests.ModuleRequest;
import com.edunexus.course.CourseRequests.ReplyRequest;
import com.edunexus.course.CourseRequests.UpdateCourseRequest;
import com.edunexus.security.AuthenticatedUser;
import com.edunexus.user.User;
import com.edunexus.user.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Course behaviour ported from reference backend/src/controllers/course.ts, with its defects corrected. */
@Service
public class CourseService {

    private final CourseRepository courses;
    private final UserRepository users;

    public CourseService(CourseRepository courses, UserRepository users) {
        this.courses = courses;
        this.users = users;
    }

    public List<Course> list() {
        return courses.findAll();
    }

    public Course get(String id) {
        return courses.findById(id).orElseThrow(() -> ApiException.notFound("Course not found"));
    }

    public Course create(CreateCourseRequest req, AuthenticatedUser actor) {
        Course c = new Course();
        c.title = req.title().trim();
        c.description = req.description().trim();
        c.adminId = actor.id();
        c.modules = toModules(req.modules());
        c.createdAt = c.updatedAt = Instant.now();
        Course saved = courses.save(c);
        users.findById(actor.id()).ifPresent(u -> {
            if (!u.managedCourses.contains(saved.id)) {
                u.managedCourses.add(saved.id);
                users.save(u);
            }
        });
        return saved;
    }

    public Course update(String id, UpdateCourseRequest req, AuthenticatedUser actor) {
        Course c = get(id);
        CoursePermissions.requireStaff(c, actor);
        if (req.title() != null) {
            if (req.title().isBlank()) throw ApiException.badRequest("Title cannot be empty");
            c.title = req.title().trim();
        }
        if (req.description() != null) {
            if (req.description().isBlank()) throw ApiException.badRequest("Description cannot be empty");
            c.description = req.description().trim();
        }
        if (req.modules() != null) {
            c.modules = toModules(req.modules());
        }
        c.updatedAt = Instant.now();
        return courses.save(c);
    }

    public void delete(String id, AuthenticatedUser actor) {
        Course c = get(id);
        CoursePermissions.requireOwnerOrAdmin(c, actor);
        courses.delete(c);
        // Clean both reference arrays (the reference only cleaned managedCourses: D12).
        for (User u : users.findByManagedCoursesContaining(id)) {
            u.managedCourses.remove(id);
            users.save(u);
        }
        for (User u : users.findByEnrolledCoursesContaining(id)) {
            u.enrolledCourses.remove(id);
            users.save(u);
        }
    }

    public Course enroll(String id, AuthenticatedUser actor) {
        Course c = get(id);
        if (c.enrolledStudents.contains(actor.id())) {
            throw ApiException.badRequest("Already enrolled in this course");
        }
        c.enrolledStudents.add(actor.id());
        c.updatedAt = Instant.now();
        Course saved = courses.save(c);
        users.findById(actor.id()).ifPresent(u -> {
            if (!u.enrolledCourses.contains(id)) {
                u.enrolledCourses.add(id);
                users.save(u);
            }
        });
        return saved;
    }

    public Course addAnnouncement(String id, AnnouncementRequest req, AuthenticatedUser actor) {
        Course c = get(id);
        CoursePermissions.requireStaff(c, actor);
        Announcement a = new Announcement();
        a.id = newId();
        a.title = req.title().trim();
        a.content = req.content().trim();
        a.authorId = actor.id();
        a.createdAt = Instant.now();
        c.announcements.add(a);
        c.updatedAt = a.createdAt;
        return courses.save(c);
    }

    public Course addDiscussion(String id, DiscussionRequest req, AuthenticatedUser actor) {
        Course c = get(id);
        CoursePermissions.requireParticipant(c, actor);
        Discussion d = new Discussion();
        d.id = newId();
        d.title = req.title().trim();
        d.content = req.content().trim();
        d.authorId = actor.id();
        d.isAnonymous = Boolean.TRUE.equals(req.isAnonymous());
        d.createdAt = Instant.now();
        c.discussions.add(d);
        c.updatedAt = d.createdAt;
        return courses.save(c);
    }

    public Course addReply(String id, String discussionId, ReplyRequest req, AuthenticatedUser actor) {
        Course c = get(id);
        CoursePermissions.requireParticipant(c, actor);
        Discussion d = c.discussions.stream().filter(x -> discussionId.equals(x.id)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Discussion not found"));
        DiscussionReply r = new DiscussionReply();
        r.id = newId();
        r.content = req.content().trim();
        r.authorId = actor.id();
        r.isAnonymous = Boolean.TRUE.equals(req.isAnonymous());
        r.createdAt = Instant.now();
        d.replies.add(r);
        c.updatedAt = r.createdAt;
        return courses.save(c);
    }

    private static List<CourseModule> toModules(List<ModuleRequest> requests) {
        List<CourseModule> modules = new ArrayList<>();
        if (requests == null) return modules;
        for (ModuleRequest mr : requests) {
            CourseModule m = new CourseModule();
            m.id = newId();
            m.title = mr.title().trim();
            m.description = mr.description().trim();
            m.dueDate = mr.dueDate();
            if (mr.contents() != null) {
                for (var cr : mr.contents()) {
                    ModuleContent mc = new ModuleContent();
                    mc.id = newId();
                    mc.title = cr.title().trim();
                    mc.type = cr.type();
                    mc.content = cr.content().trim();
                    mc.required = cr.required() == null || cr.required();
                    m.contents.add(mc);
                }
            }
            modules.add(m);
        }
        return modules;
    }

    private static String newId() {
        return new ObjectId().toHexString();
    }
}
