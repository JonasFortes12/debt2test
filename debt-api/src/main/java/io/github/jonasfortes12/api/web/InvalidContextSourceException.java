package io.github.jonasfortes12.api.web;

/**
 * The request's {@code context} object is well-formed JSON but cannot be used: an unsupported
 * provider or a malformed base URL. Answered with {@code 400} and one field error.
 */
class InvalidContextSourceException extends RuntimeException {

    private final String field;
    private final String fieldMessage;

    InvalidContextSourceException(String field, String fieldMessage, String message) {
        super(message);
        this.field = field;
        this.fieldMessage = fieldMessage;
    }

    FieldError fieldError() {
        return new FieldError(field, fieldMessage);
    }
}
