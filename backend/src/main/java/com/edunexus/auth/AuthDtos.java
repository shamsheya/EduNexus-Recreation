package com.edunexus.auth;

import com.edunexus.user.UserDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response records for the auth module (reference: RegisterRequest.java, routes/auth.ts:8-21). */
public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName,
            @NotBlank(message = "Email is required") @Email(message = "Please enter a valid email") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters long") String password,
            String role) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Please enter a valid email") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
