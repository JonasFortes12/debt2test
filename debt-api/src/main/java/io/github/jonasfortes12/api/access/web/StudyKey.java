package io.github.jonasfortes12.api.access.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Opt-in service key for the research scripts: a request carrying it in {@link #HEADER} skips the
 * access-code flow. Disabled when no key is configured, so a wrong or missing key never grants access.
 */
public final class StudyKey {

    public static final String HEADER = "X-Study-Key";
    /** OpenAPI security scheme name. */
    public static final String SCHEME = "studyKey";
    /** Request attribute the interceptor sets (to {@code true}) for a request that presented the key. */
    public static final String REQUEST_ATTRIBUTE = "studyRun";
    /** Value recorded as the run's {@code triggered_by}. */
    public static final String TRIGGER = "study";

    private final byte[] configured;

    public StudyKey(String configuredKey) {
        this.configured = configuredKey == null || configuredKey.isBlank()
                ? null
                : configuredKey.strip().getBytes(StandardCharsets.UTF_8);
    }

    public boolean accepts(String presented) {
        return configured != null && presented != null
                && MessageDigest.isEqual(configured, presented.strip().getBytes(StandardCharsets.UTF_8));
    }
}
