package io.github.jonasfortes12.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Issue tracker to read SATD context from for this run. Before the run is accepted, the "
        + "server checks it can browse the tracker with the given credentials (or anonymously when none are "
        + "given): a private Jira without credentials, rejected credentials, or a URL that is not a Jira "
        + "site answer 400; an unreachable tracker answers 502. Access is per project in Jira, so an item "
        + "can still end with contextStatus FAILED (UNAUTHORIZED) if it references a restricted project. "
        + "Credentials are used only for this run: they are never stored, logged, or returned.")
public record ContextSourceRequest(
        @Schema(description = "Issue tracker provider. Case-insensitive. Any other value answers 400 naming the "
                + "supported providers.", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
                        "jira" }, example = "jira") @NotBlank String provider,

        @Schema(description = "Base URL of the tracker site (absolute http or https URL). A trailing slash is "
                + "ignored. For Jira Cloud this is the site root.", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "https://your-company.atlassian.net") @NotBlank String baseUrl,

        @Schema(description = "Account email for a private Jira. Send it together with apiToken, or omit both "
                + "(blank counts as omitted) for a public Jira that allows anonymous browsing.", nullable = true,
                accessMode = Schema.AccessMode.WRITE_ONLY, example = "dev@your-company.com") String email,

        @Schema(description = "Jira API token for the email above. Send it together with email, or omit both "
                + "for a public Jira. Never stored, logged, or returned.", nullable = true,
                accessMode = Schema.AccessMode.WRITE_ONLY, example = "<api-token>") String apiToken) {

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "email and apiToken must be provided together")
    public boolean isCredentialPairComplete() {
        return isBlank(email) == isBlank(apiToken);
    }

    @Override
    public String toString() {
        return "ContextSourceRequest[provider=" + provider + ", baseUrl=" + baseUrl
                + ", email=" + (isBlank(email) ? "absent" : "***")
                + ", apiToken=" + (isBlank(apiToken) ? "absent" : "***") + "]";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
