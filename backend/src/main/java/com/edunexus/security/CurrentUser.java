package com.edunexus.security;

import com.edunexus.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Access to the authenticated principal set by {@link JwtAuthFilter}. */
public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthenticatedUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }
}
