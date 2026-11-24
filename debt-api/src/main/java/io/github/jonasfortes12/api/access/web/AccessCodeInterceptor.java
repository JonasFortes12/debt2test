package io.github.jonasfortes12.api.access.web;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import io.github.jonasfortes12.api.access.AccessCodeRequiredException;
import io.github.jonasfortes12.api.access.AccessCodeState;
import io.github.jonasfortes12.api.access.AccessCodeUsedException;
import io.github.jonasfortes12.api.access.AccessService;
import io.github.jonasfortes12.api.access.AuthenticatedCode;
import io.github.jonasfortes12.api.web.RunNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Guards {@code /api/pipeline/**}, except for a request carrying the study service key (see
 * {@link StudyKey}), which skips the access-code checks below. Starting a run needs an UNUSED code (the controller then spends
 * it); every endpoint that names a run needs the USED code that started that run. A code for any
 * other run gets the same 404 as an unknown run, so a code cannot probe which runs exist. Failures
 * are thrown, so they reach the client through {@code ApiExceptionHandler} as an {@code ApiError}.
 */
public class AccessCodeInterceptor implements HandlerInterceptor {

    private static final String EXECUTION_ID = "executionId";

    private final AccessService accessService;
    private final StudyKey studyKey;

    public AccessCodeInterceptor(AccessService accessService, StudyKey studyKey) {
        this.accessService = accessService;
        this.studyKey = studyKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        if (studyKey.accepts(request.getHeader(StudyKey.HEADER))) {
            request.setAttribute(StudyKey.REQUEST_ATTRIBUTE, Boolean.TRUE);
            return true;
        }
        AuthenticatedCode code = accessService.authenticate(BearerTokens.from(request.getHeader(HttpHeaders.AUTHORIZATION)))
                .filter(authenticated -> authenticated.state() != AccessCodeState.EXPIRED)
                .orElseThrow(AccessCodeRequiredException::new);
        String executionId = requestedExecutionId(request);
        if (executionId == null) {
            requireUnused(code);
        } else if (!code.grantsAccessTo(executionId)) {
            throw new RunNotFoundException(executionId);
        }
        return true;
    }

    private static void requireUnused(AuthenticatedCode code) {
        if (code.state() != AccessCodeState.UNUSED) {
            throw new AccessCodeUsedException();
        }
    }

    @SuppressWarnings("unchecked")
    private static String requestedExecutionId(HttpServletRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return variables instanceof Map<?, ?> map ? ((Map<String, String>) map).get(EXECUTION_ID) : null;
    }
}
