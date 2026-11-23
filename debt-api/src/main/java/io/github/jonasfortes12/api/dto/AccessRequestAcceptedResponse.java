package io.github.jonasfortes12.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Always the same answer, whatever happened to the request, so it never reveals whether an "
        + "address is known, has reached its quota, or is waiting out the cooldown.")
public record AccessRequestAcceptedResponse(
        @Schema(description = "Fixed text, safe to display.",
                example = "If the address can receive email, a message has been sent to it.") String message) {

    public static final String MESSAGE = "If the address can receive email, a message has been sent to it.";

    public static AccessRequestAcceptedResponse accepted() {
        return new AccessRequestAcceptedResponse(MESSAGE);
    }
}
