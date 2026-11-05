package io.github.jonasfortes12.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A run that was recorded and handed to the worker.")
public record RunAcceptedResponse(
                @Schema(description = "UUID of this run; the identifier every other endpoint takes.", example = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40") String executionId) {
}
