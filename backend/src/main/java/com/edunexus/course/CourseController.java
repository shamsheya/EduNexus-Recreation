package com.edunexus.course;

import com.edunexus.course.CourseRequests.AnnouncementRequest;
import com.edunexus.course.CourseRequests.CreateCourseRequest;
import com.edunexus.course.CourseRequests.DiscussionRequest;
import com.edunexus.course.CourseRequests.ReplyRequest;
import com.edunexus.course.CourseRequests.UpdateCourseRequest;
import com.edunexus.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Endpoint set from reference backend/src/routes/course.ts:15-22 plus the content endpoints
 * declared in reference frontend/src/services/api.ts:82-98. Global role checks use
 * method security; course-scoped checks live in {@link CoursePermissions}.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<Course> list() {
        return courseService.list();
    }

    @GetMapping("/{id}")
    public Course get(@PathVariable String id) {
        return courseService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Course create(@Valid @RequestBody CreateCourseRequest request) {
        return courseService.create(request, CurrentUser.get());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Course update(@PathVariable String id, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request, CurrentUser.get());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
    public Map<String, String> delete(@PathVariable String id) {
        courseService.delete(id, CurrentUser.get());
        return Map.of("message", "Course deleted successfully");
    }

    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public Course enroll(@PathVariable String id) {
        return courseService.enroll(id, CurrentUser.get());
    }

    @PostMapping("/{id}/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public Course addAnnouncement(@PathVariable String id, @Valid @RequestBody AnnouncementRequest request) {
        return courseService.addAnnouncement(id, request, CurrentUser.get());
    }

    @PostMapping("/{id}/discussions")
    @ResponseStatus(HttpStatus.CREATED)
    public Course addDiscussion(@PathVariable String id, @Valid @RequestBody DiscussionRequest request) {
        return courseService.addDiscussion(id, request, CurrentUser.get());
    }

    @PostMapping("/{id}/discussions/{discussionId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public Course addReply(@PathVariable String id, @PathVariable String discussionId,
                           @Valid @RequestBody ReplyRequest request) {
        return courseService.addReply(id, discussionId, request, CurrentUser.get());
    }
}
