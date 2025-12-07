package it.polimi.se.bbp.security;

import it.polimi.se.bbp.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Authentication Filter that validates JWT tokens in HTTP requests.
 * Runs once per request, checks Authorization header for valid JWT tokens.
 * Stores userId as principal in SecurityContext.
 * All authenticated users have ROLE_USER by default.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * Service for JWT token operations (extraction, validation, generation).
     */
    private final JwtService jwtService;

    /**
     * Repository to verify user existence.
     * Ensures deleted users cannot access API even with valid tokens.
     */
    private final UserRepository userRepository;

    /**
     * Filters incoming requests to validate JWT tokens.
     * If valid token found, userId stored in security context as principal.
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        // Check if Authorization header is present and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        // Extract JWT token from header (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);
        // Extract userId from token
        final Long userId = jwtService.extractUserId(jwt);
        // If userId is extracted and user is not already authenticated
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Validate token (checks signature and expiration)
            if (jwtService.isTokenValid(jwt, userId)) {
                // Verify user still exists in database
                if (userRepository.existsById(userId)) {
                    // Create authentication token with userId as principal
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    // Set additional request details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Store authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        // Continue filter chain
        filterChain.doFilter(request, response);
    }

}