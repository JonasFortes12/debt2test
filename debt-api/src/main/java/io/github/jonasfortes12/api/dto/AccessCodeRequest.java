package io.github.jonasfortes12.api.dto;

import io.github.jonasfortes12.api.access.EmailAddresses;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Asks for an access code. Sending it records consent to the given terms version, together "
                + "with the time and the client IP address.")
public record AccessCodeRequest(
                @Schema(description = "Participant's name, kept as research data as described in the terms. ", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200, example = "Ana Souza") @NotBlank @Size(max = 200) String name,

                @Schema(description = "Where the code is sent. Compared case-insensitively (trimmed and lower-cased); "
                                + "the run quota is counted per address.", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 320, example = "ana@example.com") @NotBlank @Email @Size(max = 320) String email,

                @Schema(description = "The terms version the participant read and accepted; must equal the current "
                                + "version from GET /api/access/terms, otherwise 400.", requiredMode = Schema.RequiredMode.REQUIRED, example = "v1") @NotBlank String acceptedTermsVersion) {

        @Override
        public String toString() {
                String maskedEmail = email == null ? "absent" : EmailAddresses.mask(EmailAddresses.normalize(email));
                return "AccessCodeRequest[name=" + name + ", email=" + maskedEmail
                                + ", acceptedTermsVersion=" + acceptedTermsVersion + "]";
        }
}
