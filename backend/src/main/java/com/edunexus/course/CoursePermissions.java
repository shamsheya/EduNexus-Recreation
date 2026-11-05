package com.edunexus.course;

import com.edunexus.common.ApiException;
import com.edunexus.security.AuthenticatedUser;
import com.edunexus.user.Role;

/**
 * Course-scoped authorization, ported from the reference ownership checks
 * (backend/src/controllers/course.ts:70-73 and :97-100) and role-permission-matrix.md section 5.
 */
final class CoursePermissions {
    private CoursePermissions() {
    }

    static boolean isOwner(Course c, AuthenticatedUser u) {
        return u.id().equals(c.adminId);
    }

    static boolean isTa(Course c, AuthenticatedUser u) {
        return c.teachingAssistants.contains(u.id());
    }

    static boolean isEnrolled(Course c, AuthenticatedUser u) {
        return c.enrolledStudents.contains(u.id());
    }

    static boolean isAdmin(AuthenticatedUser u) {
        return u.hasRole(Role.ADMIN);
    }

    /** Edit course / post announcements: owner, TA or admin. */
    static void requireStaff(Course c, AuthenticatedUser u) {
        if (!(isAdmin(u) || isOwner(c, u) || isTa(c, u))) {
            throw ApiException.forbidden("Not authorized");
        }
    }

    /** Delete course: owner or admin (decision OD2). */
    static void requireOwnerOrAdmin(Course c, AuthenticatedUser u) {
        if (!(isAdmin(u) || isOwner(c, u))) {
            throw ApiException.forbidden("Not authorized");
        }
    }

    /** Participate in discussions: enrolled student, owner, TA or admin. */
    static void requireParticipant(Course c, AuthenticatedUser u) {
        if (!(isAdmin(u) || isOwner(c, u) || isTa(c, u) || isEnrolled(c, u))) {
            throw ApiException.forbidden("You must be enrolled in this course");
        }
    }
}
