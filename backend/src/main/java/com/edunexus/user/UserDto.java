package com.edunexus.user;

import java.util.List;

/** User shape expected by the frontend (reference authSlice.ts:5-13). Never contains the password hash. */
public record UserDto(
        String id,
        String email,
        String firstName,
        String lastName,
        List<Role> roles,
        List<String> enrolledCourses,
        List<String> managedCourses) {

    public static UserDto from(User u) {
        return new UserDto(u.id, u.email, u.firstName, u.lastName,
                List.copyOf(u.roles), List.copyOf(u.enrolledCourses), List.copyOf(u.managedCourses));
    }
}
