package com.edunexus.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Uniform error envelope: {"message": "...", "errors": {"field": "reason"}} */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, Map<String, String> errors) {
    public static ApiError of(String message) {
        return new ApiError(message, null);
    }
}
