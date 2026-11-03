package io.github.jonasfortes12.api.web;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.jonasfortes12.core.error.PipelineException;
import io.github.jonasfortes12.orchestrator.util.OrchestrationUtils;

/**
 * Maps every failure to an {@link ApiError}, so clients parse one error shape.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(RunNotFoundException.class)
    ResponseEntity<ApiError> notFound(RunNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler({ RunNotFinishedException.class, RunAlreadyFinishedException.class })
    ResponseEntity<ApiError> conflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ApiError("invalid request body", fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiError.of("malformed JSON request body"));
    }

    @ExceptionHandler(PipelineException.class)
    ResponseEntity<ApiError> persistenceUnavailable(PipelineException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of("run could not be started: persistence is unavailable"));
    }

    /**
     * Spring's own request failures (405, 415, unknown path, ...) keep their
     * status; anything else
     * is a 500 whose message reveals nothing about the cause, which is only logged.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        if (exception instanceof ErrorResponse springFailure) {
            HttpStatusCode status = springFailure.getStatusCode();
            return ResponseEntity.status(status).body(ApiError.of(reasonPhrase(status)));
        }
        OrchestrationUtils.logPipeline("unexpected API failure: " + exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of("internal server error"));
    }

    private static String reasonPhrase(HttpStatusCode status) {
        HttpStatus known = HttpStatus.resolve(status.value());
        return known == null ? "request failed" : known.getReasonPhrase().toLowerCase(Locale.ROOT);
    }
}
