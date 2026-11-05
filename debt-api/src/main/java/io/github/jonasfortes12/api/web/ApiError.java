package io.github.jonasfortes12.api.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Body of every non-2xx response.")
public record ApiError(
        @Schema(description = "Human-readable reason, safe to display.", example = "invalid request body") String message,

        @Schema(description = "Fields that failed validation; empty for any "
                + "other kind of error. Never null.") List<FieldError> fieldErrors) {

    public ApiError {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ApiError of(String message) {
        return new ApiError(message, List.of());
    }
}
