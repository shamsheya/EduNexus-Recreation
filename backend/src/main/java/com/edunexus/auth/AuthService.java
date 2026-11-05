package com.edunexus.auth;

import com.edunexus.auth.AuthDtos.AuthResponse;
import com.edunexus.auth.AuthDtos.LoginRequest;
import com.edunexus.auth.AuthDtos.RegisterRequest;
import com.edunexus.common.ApiException;
import com.edunexus.security.JwtService;
import com.edunexus.user.Role;
import com.edunexus.user.User;
import com.edunexus.user.UserDto;
import com.edunexus.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest req) {
        Role role = parseSelfRegistrationRole(req.role());
        String email = normaliseEmail(req.email());
        if (users.existsByEmail(email)) {
            throw ApiException.badRequest("Email already exists");
        }
        User user = new User();
        user.email = email;
        user.passwordHash = passwordEncoder.encode(req.password());
        user.firstName = req.firstName().trim();
        user.lastName = req.lastName().trim();
        user.roles.add(role);
        users.save(user);
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normaliseEmail(req.email()))
                .filter(u -> passwordEncoder.matches(req.password(), u.passwordHash))
                // 400 (not 401) as in the reference Express backend, so the frontend's
                // 401 interceptor does not redirect away from the login form.
                .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));
        return new AuthResponse(jwtService.generateToken(user), UserDto.from(user));
    }

    /** Decision OD1: admin cannot be self-registered; student is the default (reference Express default). */
    static Role parseSelfRegistrationRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return Role.STUDENT;
        }
        Role role;
        try {
            role = Role.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Invalid role. Must be one of: student, instructor");
        }
        if (!Set.of(Role.STUDENT, Role.INSTRUCTOR).contains(role)) {
            throw ApiException.badRequest("Admin accounts cannot be self-registered");
        }
        return role;
    }

    static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
