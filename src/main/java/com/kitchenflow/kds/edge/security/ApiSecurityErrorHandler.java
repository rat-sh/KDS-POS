package com.kitchenflow.kds.edge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kitchenflow.kds.edge.error.ApiExceptionHandler.ErrorBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final Clock clock;

    public ApiSecurityErrorHandler(ObjectMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException {
        write(request, response, 401, "UNAUTHENTICATED", "Authentication is required.");
        response.setHeader("WWW-Authenticate", "Bearer");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException failure)
            throws IOException {
        write(request, response, 403, "FORBIDDEN", "You do not have permission to perform this action.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, int status, String code, String message)
            throws IOException {
        Object correlation = request.getAttribute("correlationId");
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorBody(clock.instant(), status, code, message,
                request.getRequestURI(), correlation == null ? "" : correlation.toString(), Map.of(), List.of()));
    }
}
