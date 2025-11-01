package it.polimi.se.bbp.config;

import it.polimi.se.bbp.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
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
     * Injected via constructor by Lombok's @RequiredArgsConstructor.
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Service for loading user-specific data during authentication.
     * Injected via constructor by Lombok's @RequiredArgsConstructor.
     */
    private final UserDetailsService userDetailsService;

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
     * Configures the security filter chain.
     * Defines authentication, authorization, CORS, and session management policies.
     * @param http HttpSecurity object
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(this::configureAuthorization)
                .sessionManagement(this::configureSessionManagement)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures authorization rules for HTTP requests.
     * Defines which endpoints are public and which require authentication.
     * @param auth AuthorizationManagerRequestMatcherRegistry
     */
    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Public endpoints - accessible without authentication
                .requestMatchers("/api/auth/**").permitAll()
                // Public read-only endpoints for bike paths, obstacles, and search

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
     * Configures the authentication provider with user details service and password encoder.
     * @return configured AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Creates the authentication manager bean.
     * @param config authentication configuration
     * @return AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
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