package com.edunexus.user;

import com.edunexus.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Reference: backend/src/routes/user.ts:10-12 (all routes require authentication; self only). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserDto getProfile() {
        return userService.getProfile(CurrentUser.get().id());
    }

    @PutMapping("/profile")
    public UserDto updateProfile(@Valid @RequestBody UserRequests.ProfileUpdateRequest request) {
        return userService.updateProfile(CurrentUser.get().id(), request);
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@Valid @RequestBody UserRequests.PasswordChangeRequest request) {
        userService.changePassword(CurrentUser.get().id(), request);
        return Map.of("message", "Password updated successfully");
    }
}
