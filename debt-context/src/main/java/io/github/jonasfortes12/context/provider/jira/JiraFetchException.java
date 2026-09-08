package io.github.jonasfortes12.context.provider.jira;

import java.util.regex.Pattern;

/**
 * Signals that a Jira issue lookup failed for a reason other than "issue does not exist"
 * (that case is represented by {@link JiraClient#getIssue(String)} returning {@code null}).
 *
 * <p>{@link #code()} is this module's internal taxonomy and is intentionally more specific than
 * what ends up in a report: {@code ContextHandler.safeErrorReason()} sanitizes every provider
 * error code down to one of {@code NOT_FOUND}, {@code RATE_LIMITED}, {@code UNAUTHORIZED}, or
 * {@code UNKNOWN} before it reaches {@code PipelineError}, and {@code FileReportSink} only ever
 * emits {@code CONTEXT_PROVIDER_JIRA_(RATE_LIMITED|UNAUTHORIZED|UNKNOWN)} in the final report.
 * The {@link #recoverable()} flag survives that sanitization untouched, so it is still worth
 * setting accurately here even for codes that will collapse to {@code UNKNOWN}.
 */
public final class JiraFetchException extends Exception {

    private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");

    private final String code;
    private final boolean recoverable;

    public JiraFetchException(String code, boolean recoverable) {
        super("Jira issue fetch failed.");
        if (code == null || !SAFE_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("error code must be a safe identifier");
        }
        this.code = code;
        this.recoverable = recoverable;
    }

    public String code() {
        return code;
    }

    public boolean recoverable() {
        return recoverable;
    }
}
