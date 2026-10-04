package com.antonlappa.rechnungapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Stateless JWT authentication filter.
 * <p>
 * Runs once per request, extracts the Bearer token from the
 * {@code Authorization} header, validates it, and sets the
 * {@link SecurityContextHolder} so downstream filters and
 * controllers see an authenticated principal.
 * <p>
 * Invalid or expired JWTs are forwarded to the
 * {@link com.antonlappa.rechnungapp.exception.exceptionhandler.GlobalExceptionHandler}
 * via {@link HandlerExceptionResolver}, ensuring a consistent 401 JSON response.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No Bearer token, or an empty/blank one → skip JWT processing (entry point answers 401)
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)
                || authHeader.substring(BEARER_PREFIX.length()).isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(authHeader.substring(BEARER_PREFIX.length()), request);
        } catch (Exception ex) {
            // Forward JWT/user-lookup failures (ExpiredJwtException, UsernameNotFoundException, etc.)
            // to GlobalExceptionHandler; the request does not continue down the chain
            SecurityContextHolder.clearContext();
            handlerExceptionResolver.resolveException(request, response, null, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String jwt, HttpServletRequest request) {
        final String userEmail = jwtService.extractUsername(jwt);

        // Only authenticate if not already set in the SecurityContext
        if (userEmail == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

        if (jwtService.isTokenValid(jwt, userDetails)) {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
}
