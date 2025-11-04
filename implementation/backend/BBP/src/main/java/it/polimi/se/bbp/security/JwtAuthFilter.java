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
 * JWT Authentication Filter that intercepts HTTP requests to validate JWT tokens.
 * This filter runs once per request and checks for valid JWT tokens in the Authorization header.
 * Stores userId as principal in SecurityContext for efficient retrieval.
 * All authenticated users have ROLE_USER by default.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * Service for handling JWT token operations such as extraction, validation, and generation.
     */
    private final JwtService jwtService;

    /**
     * Repository to verify user existence.
     * This ensures that deleted users cannot access the API even with valid tokens.
     */
    private final UserRepository userRepository;

    /**
     * Filters incoming requests to validate JWT tokens.
     * If a valid token is found, the userId is stored in the security context as principal.
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
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