package io.github.jonasfortes12.api.dto;

import java.time.Instant;

import io.github.jonasfortes12.api.access.AccessCodeState;
import io.github.jonasfortes12.api.access.AuthenticatedCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What an access code currently allows. Contains nothing personal.")
public record AccessCodeStatusResponse(
        @Schema(description = "UNUSED: can start one run. USED: already started its run, and still reads, polls "
                + "and cancels that run. EXPIRED: grants nothing anymore (unused past its expiry, replaced by a "
                + "newer code for the same email, or past the 30-day results window).",
                allowableValues = { "UNUSED", "USED", "EXPIRED" }, example = "USED") AccessCodeState state,

        @Schema(description = "When the current state ends: the code's expiry while UNUSED (72 hours after it "
                + "was issued), the end of the results window while USED (30 days after the run started), and "
                + "the last of those instants once EXPIRED.", example = "2026-12-06T12:00:00Z") Instant expiresAt,

        @Schema(description = "The run this code started; null until the code is used.", nullable = true,
                example = "3f2b8c1e-9a4d-4e7b-8f6a-2c1d5e9b7a40") String executionId) {

    public static AccessCodeStatusResponse from(AuthenticatedCode code) {
        String executionId = code.executionId() == null ? null : code.executionId().toString();
        return new AccessCodeStatusResponse(code.state(), code.expiresAt(), executionId);
    }
}
