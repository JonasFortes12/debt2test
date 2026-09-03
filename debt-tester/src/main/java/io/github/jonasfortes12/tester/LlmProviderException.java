package io.github.jonasfortes12.tester;

import java.util.regex.Pattern;

public final class LlmProviderException extends Exception {

    private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");

    private final String code;
    private final boolean recoverable;

    public LlmProviderException(String code, boolean recoverable) {
        super("LLM provider operation failed.");
        if (code == null || !SAFE_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("provider error code must be a safe identifier");
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
