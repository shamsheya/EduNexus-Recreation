package com.edunexus.user;

import com.edunexus.common.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Profile operations ported from reference backend/src/controllers/user.ts. */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User require(String id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
    }

    public UserDto getProfile(String id) {
        return UserDto.from(require(id));
    }

    public UserDto updateProfile(String id, UserRequests.ProfileUpdateRequest req) {
        User user = require(id);
        if (req.firstName() != null && !req.firstName().isBlank()) {
            user.firstName = req.firstName().trim();
        }
        if (req.lastName() != null && !req.lastName().isBlank()) {
            user.lastName = req.lastName().trim();
        }
        if (req.email() != null && !req.email().isBlank()) {
            String email = req.email().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(user.email) && users.existsByEmail(email)) {
                throw ApiException.badRequest("Email already exists");
            }
            user.email = email;
        }
        return UserDto.from(users.save(user));
    }

    public void changePassword(String id, UserRequests.PasswordChangeRequest req) {
        User user = require(id);
        if (!passwordEncoder.matches(req.currentPassword(), user.passwordHash)) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.passwordHash = passwordEncoder.encode(req.newPassword());
        users.save(user);
    }
}
