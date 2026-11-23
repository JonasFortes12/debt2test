package io.github.jonasfortes12.api.access.web;

/** Name of the OpenAPI security scheme that documents {@code Authorization: Bearer <access code>}. */
public final class AccessSecurity {

    public static final String SCHEME = "accessCode";

    private AccessSecurity() {
    }
}
