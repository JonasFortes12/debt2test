package io.github.jonasfortes12.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.jonasfortes12.core.error.PipelineException;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(RunNotFoundException.class)
    ResponseEntity<ApiError> notFound(RunNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler({RunNotFinishedException.class, RunAlreadyFinishedException.class})
    ResponseEntity<ApiError> conflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> badRequest(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiError("invalid request body"));
    }

    @ExceptionHandler(PipelineException.class)
    ResponseEntity<ApiError> persistenceUnavailable(PipelineException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("run could not be started: persistence is unavailable"));
    }
}
