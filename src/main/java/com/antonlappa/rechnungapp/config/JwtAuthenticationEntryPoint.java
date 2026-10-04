package com.antonlappa.rechnungapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Answers unauthenticated requests to protected endpoints with 401.
 * <p>
 * Without a custom entry point Spring Security falls back to
 * {@code Http403ForbiddenEntryPoint}, so a missing token would yield 403.
 * The exception is forwarded to the
 * {@link com.antonlappa.rechnungapp.exception.exceptionhandler.GlobalExceptionHandler}
 * via {@link HandlerExceptionResolver} to keep the JSON error format consistent.
 * Authenticated users lacking permission still get 403 from the default access-denied handler.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAuthenticationEntryPoint(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) {
        handlerExceptionResolver.resolveException(request, response, null, authException);
    }
}
