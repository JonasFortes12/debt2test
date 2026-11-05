package io.github.jonasfortes12.api.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One request field that failed validation.")
public record FieldError(
                @Schema(description = "Name of the request field.", example = "repositoryUrl") String field,

                @Schema(description = "Why the value was rejected.", example = "must not be blank") String message) {
}
