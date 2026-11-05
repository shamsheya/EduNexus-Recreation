package com.edunexus.course;

import com.edunexus.course.Course.ContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/** Request bodies. Field whitelist replaces the reference's `$set: req.body` mass assignment (controllers/course.ts:75-79). */
public final class CourseRequests {
    private CourseRequests() {
    }

    public record ContentRequest(
            @NotBlank(message = "Content title is required") String title,
            @NotNull(message = "Content type is required") ContentType type,
            @NotBlank(message = "Content is required") String content,
            Boolean required) {
    }

    public record ModuleRequest(
            @NotBlank(message = "Module title is required") String title,
            @NotBlank(message = "Module description is required") String description,
            @NotNull(message = "Module due date is required") Instant dueDate,
            List<@Valid ContentRequest> contents) {
    }

    public record CreateCourseRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "Description is required") String description,
            List<@Valid ModuleRequest> modules) {
    }

    /** Partial update: null fields are left unchanged. */
    public record UpdateCourseRequest(
            String title,
            String description,
            List<@Valid ModuleRequest> modules) {
    }

    public record AnnouncementRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "Content is required") String content) {
    }

    public record DiscussionRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "Content is required") String content,
            Boolean isAnonymous) {
    }

    public record ReplyRequest(
            @NotBlank(message = "Content is required") String content,
            Boolean isAnonymous) {
    }
}
