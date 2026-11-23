package io.github.jonasfortes12.api.access.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.jonasfortes12.api.access.AccessCodeRequiredException;
import io.github.jonasfortes12.api.access.AccessService;
import io.github.jonasfortes12.api.dto.AccessCodeRequest;
import io.github.jonasfortes12.api.dto.AccessCodeStatusResponse;
import io.github.jonasfortes12.api.dto.AccessRequestAcceptedResponse;
import io.github.jonasfortes12.api.dto.TermsResponse;
import io.github.jonasfortes12.api.web.ApiError;
import io.github.jonasfortes12.api.web.ApiExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/access")
@Tag(name = "Access", description = "Read the terms, ask for a single-run access code by email, and check a code.")
public class AccessController {

    private static final String JSON = "application/json";

    private final AccessService accessService;
    private final TermsCatalog terms;

    public AccessController(AccessService accessService, TermsCatalog terms) {
        this.accessService = accessService;
        this.terms = terms;
    }

    @GetMapping("/terms")
    @Operation(summary = "Get the current terms of use", description = "Show this text to the participant before "
            + "asking for a code; the request must send back its version as acceptedTermsVersion. Needs no access code.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The current terms version and its Markdown text.",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = TermsResponse.class),
                            examples = @ExampleObject(value = ApiExamples.TERMS)))
    })
    public TermsResponse terms() {
        return new TermsResponse(terms.version(), terms.text());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Ask for an access code by email", description = "Records the participant and their "
            + "consent to the given terms version, then emails a new single-run code valid for 72 hours. Asking "
            + "again replaces any unused code with a new one. When the address has already started its maximum "
            + "number of runs, the email explains that instead of carrying a code. Repeated requests for the same "
            + "address within a few minutes of the last email (code or notice) send nothing. The answer is identical in all these cases, so it never "
            + "reveals whether an address is known. Needs no access code.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Request handled; check the inbox.",
                    content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = AccessRequestAcceptedResponse.class),
                            examples = @ExampleObject(value = ApiExamples.ACCESS_REQUEST_ACCEPTED))),
            @ApiResponse(responseCode = "400", description = "The body is not valid JSON or fails validation "
                    + "(fieldErrors lists each failing field), or acceptedTermsVersion is not the current version.",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "termsNotCurrent", value = ApiExamples.TERMS_NOT_CURRENT))),
            @ApiResponse(responseCode = "415", description = "Content-Type is not application/json.",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Too many code requests from this IP address in the "
                    + "last hour; try again later.", content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ApiExamples.TOO_MANY_ACCESS_REQUESTS))),
            @ApiResponse(responseCode = "503", description = "The database is unavailable; nothing was recorded "
                    + "or sent. Retry later.", content = @Content(mediaType = JSON,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ApiExamples.DATABASE_UNAVAILABLE)))
    })
    public AccessRequestAcceptedResponse requestCode(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Participant name, email, and the "
                    + "accepted terms version.", content = @Content(mediaType = JSON,
                            examples = @ExampleObject(value = ApiExamples.ACCESS_CODE_REQUEST)))
            @Valid @RequestBody AccessCodeRequest request,
            HttpServletRequest httpRequest) {
        accessService.requestCode(request.name(), request.email(), request.acceptedTermsVersion(),
                httpRequest.getRemoteAddr());
        return AccessRequestAcceptedResponse.accepted();
    }

    @GetMapping("/code")
    @SecurityRequirement(name = AccessSecurity.SCHEME)
    @Operation(summary = "Check what an access code allows", description = "Send the code as "
            + "`Authorization: Bearer <code>`. Answers for UNUSED, USED and EXPIRED codes alike; only a missing or "
            + "unknown code is refused. A USED code's executionId is the only run it can read.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The code's state, when that state ends, and its run.",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = AccessCodeStatusResponse.class),
                            examples = @ExampleObject(value = ApiExamples.ACCESS_CODE_STATUS))),
            @ApiResponse(responseCode = "401", description = "No Bearer code was sent, or the code is unknown.",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ApiExamples.ACCESS_CODE_REQUIRED))),
            @ApiResponse(responseCode = "503", description = "The database is unavailable. Retry later.",
                    content = @Content(mediaType = JSON, schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(value = ApiExamples.DATABASE_UNAVAILABLE)))
    })
    public AccessCodeStatusResponse codeStatus(
            @Parameter(hidden = true) @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return accessService.authenticate(BearerTokens.from(authorization))
                .map(AccessCodeStatusResponse::from)
                .orElseThrow(AccessCodeRequiredException::new);
    }
}
