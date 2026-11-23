package io.github.jonasfortes12.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The current terms of use. Show the text before asking for an access code, and send its "
        + "version back as acceptedTermsVersion.")
public record TermsResponse(
        @Schema(description = "Identifier of this terms text; it changes whenever the text changes.",
                example = "v1") String version,

        @Schema(description = "Full terms text in Markdown.", example = "# Debt2Test terms of use (v1)\\n...") String text) {
}
