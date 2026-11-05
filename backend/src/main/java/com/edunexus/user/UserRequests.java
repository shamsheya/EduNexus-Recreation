package com.edunexus.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserRequests {
    private UserRequests() {
    }

    /** All fields optional, as in reference controllers/user.ts:27-37. */
    public record ProfileUpdateRequest(
            @Size(max = 100, message = "First name is too long") String firstName,
            @Size(max = 100, message = "Last name is too long") String lastName,
            @Email(message = "Please enter a valid email") String email) {
    }

    public record PasswordChangeRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 6, message = "Password must be at least 6 characters long") String newPassword) {
    }
}
