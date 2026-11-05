package com.edunexus.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Global roles. Serialised in lowercase to match the reference frontend (types.ts:1). */
public enum Role {
    STUDENT, INSTRUCTOR, ADMIN;

    @JsonValue
    public String json() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static Role fromJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid role: " + value);
        }
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
