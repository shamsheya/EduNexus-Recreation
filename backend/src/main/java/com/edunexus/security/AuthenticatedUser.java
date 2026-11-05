package com.edunexus.security;

import com.edunexus.user.Role;

import java.util.Set;

/** Principal stored in the SecurityContext for an authenticated request. */
public record AuthenticatedUser(String id, String email, Set<Role> roles) {
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
