package io.github.jonasfortes12.api.access;

/** The request accepted a terms version other than the current one. Maps to 400 on acceptedTermsVersion. */
public class TermsNotCurrentException extends RuntimeException {

    private final String currentVersion;

    public TermsNotCurrentException(String accepted, String current) {
        super("terms version '%s' is not current; current is '%s'".formatted(accepted, current));
        this.currentVersion = current;
    }

    public String currentVersion() {
        return currentVersion;
    }
}
