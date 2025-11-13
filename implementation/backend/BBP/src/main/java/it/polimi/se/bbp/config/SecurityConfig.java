package it.polimi.se.bbp.config;

import it.polimi.se.bbp.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application.
 * Configures JWT-based authentication, authorization, CORS, and security policies.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Custom JWT authentication filter that intercepts requests to validate JWT tokens.
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Comma-separated list of allowed origins for CORS configuration.
     * Read from application.properties: security.cors.allowed-origins
     */
    @Value("${security.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * BCrypt password encoding strength (computational cost).
     * Read from application.properties: security.password.bcrypt-strength
     */
    @Value("${security.password.bcrypt-strength}")
    private int bcryptStrength;

    /**
     * Flag to enable/disable H2 console access.
     * Read from application.properties: spring.h2.console.enabled
     */
    @Value("${spring.h2.console.enabled}")
    private boolean h2ConsoleEnabled;

    /**
     * Configures the security filter chain.
     * Defines authentication, authorization, CORS, and session management policies.
     * @param http HttpSecurity object
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(this::configureAuthorization)
                .sessionManagement(this::configureSessionManagement)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        // Configure headers for H2 console only if enabled
        if (h2ConsoleEnabled) {
            httpSecurity.headers(headers -> headers
                    .frameOptions(frame -> frame.sameOrigin())
            );
        }
        return httpSecurity.build();
    }

    /**
     * Configures authorization rules for HTTP requests.
     * Defines which endpoints are public and which require authentication.
     * @param auth AuthorizationManagerRequestMatcherRegistry
     */
    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // Allow H2 console access only if enabled (development)
        if (h2ConsoleEnabled) {
            auth.requestMatchers("/h2-console/**").permitAll();
        }
        auth
                // Public endpoints - accessible without authentication
                .requestMatchers("/api/auth/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated();
    }

    /**
     * Configures session management to be stateless (for JWT).
     * @param session SessionManagementConfigurer
     */
    private void configureSessionManagement(SessionManagementConfigurer<HttpSecurity> session) {
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    /**
     * Configures CORS (Cross-Origin Resource Sharing) settings.
     * Allows the frontend to communicate with the backend from a different origin.
     * @return CorsConfigurationSource with configured CORS policies
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Parse allowed origins from configuration (comma-separated list)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allowed headers (Authorization is crucial for JWT)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configures the password encoder using BCrypt.
     * The strength can be configured via application properties.
     * @return BCryptPasswordEncoder with configured strength
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    /**
     * Custom authentication entry point for handling unauthorized access (401).
     * Returns a JSON response when authentication fails or token is missing/invalid.
     * @return AuthenticationEntryPoint
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                    LocalDateTime.now(),
                    authException.getMessage(),
                    request.getRequestURI()
            ));
        };
    }

    /**
     * Custom access denied handler for handling forbidden access (403).
     * Returns a JSON response when a user tries to access a resource without proper permissions.
     * @return AccessDeniedHandler
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"%s\",\"path\":\"%s\"}",
                    LocalDateTime.now(),
                    accessDeniedException.getMessage(),
                    request.getRequestURI()
            ));
        };
    }

}