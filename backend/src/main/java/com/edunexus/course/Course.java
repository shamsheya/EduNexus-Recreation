package com.edunexus.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mirrors the reference `courses` collection with embedded modules/contents, announcements,
 * discussions and replies (reference models/Course.ts, model/Course.java; database-schema.md section 2).
 * Serialised directly as the API's Course DTO: ids are strings named "id".
 */
@Document(collection = "courses")
public class Course {
    @Id
    public String id;
    public String title;
    public String description;

    @Indexed
    public String adminId;

    @Indexed
    public List<String> enrolledStudents = new ArrayList<>();
    public List<String> teachingAssistants = new ArrayList<>();
    public List<CourseModule> modules = new ArrayList<>();
    public List<Announcement> announcements = new ArrayList<>();
    public List<Discussion> discussions = new ArrayList<>();
    public Instant createdAt;
    public Instant updatedAt;

    public static class CourseModule {
        public String id;
        public String title;
        public String description;
        public Instant dueDate;
        public List<ModuleContent> contents = new ArrayList<>();
    }

    public static class ModuleContent {
        public String id;
        public String title;
        public ContentType type;
        /** URL or text content (reference model/Course.java:46). */
        public String content;
        public boolean required = true;
    }

    public static class Announcement {
        public String id;
        public String title;
        public String content;
        public String authorId;
        public Instant createdAt;
    }

    public static class Discussion {
        public String id;
        public String title;
        public String content;
        public String authorId;
        public boolean isAnonymous;
        public List<DiscussionReply> replies = new ArrayList<>();
        public Instant createdAt;
    }

    public static class DiscussionReply {
        public String id;
        public String content;
        public String authorId;
        public boolean isAnonymous;
        public Instant createdAt;
    }

    /** Stored as upper-case enum names; serialised lower-case for the frontend icon mapping (D11). */
    public enum ContentType {
        VIDEO, DOCUMENT, ASSIGNMENT, QUIZ, REFERENCE_BOOK;

        @JsonValue
        public String json() {
            return name().toLowerCase(Locale.ROOT);
        }

        @JsonCreator
        public static ContentType fromJson(String value) {
            return value == null ? null : ContentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        }
    }
}
