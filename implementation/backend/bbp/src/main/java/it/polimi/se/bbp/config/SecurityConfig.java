package it.polimi.se.bbp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.se.bbp.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
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
import java.util.Map;

/**
 * Security configuration for JWT-based authentication and authorization.
 * Configures stateless session management, CORS policies, public/protected endpoints,
 * and custom error handlers for unauthorized and forbidden access.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * JWT authentication filter that validates tokens on incoming requests.
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * ObjectMapper for serializing JSON error responses.
     */
    private final ObjectMapper objectMapper;

    /**
     * Comma-separated list of allowed origins for CORS.
     */
    @Value("${security.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * BCrypt password encoding strength (computational cost factor).
     */
    @Value("${security.password.bcrypt-strength}")
    private int bcryptStrength;

    /**
     * Flag to enable H2 console access for development.
     */
    @Value("${spring.h2.console.enabled}")
    private boolean h2ConsoleEnabled;

    /**
     * Configures the main security filter chain.
     * Sets up CORS, disables CSRF, defines authorization rules, enables stateless sessions,
     * adds JWT filter, and configures custom exception handlers.
     * Conditionally enables H2 console frame options if h2ConsoleEnabled is true.
     * @param http HttpSecurity builder
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
                    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            );
        }
        return httpSecurity.build();
    }

    /**
     * Configures authorization rules for HTTP endpoints.
     * Public endpoints: auth, finder bike-paths, mapbox access-token, H2 console (if enabled).
     * All other endpoints require authentication.
     * @param auth authorization registry
     */
    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        // Allow H2 console access only if enabled (development)
        if (h2ConsoleEnabled) {
            auth.requestMatchers("/h2-console/**").permitAll();
        }
        // Public endpoints - accessible without authentication
        auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/finder/bike-paths/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/bike-paths/*").permitAll()
                .requestMatchers("/api/mapbox/access-token/**").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated();
    }

    /**
     * Configures stateless session management for JWT authentication.
     * No server-side session storage is used.
     * @param session session management configurer
     */
    private void configureSessionManagement(SessionManagementConfigurer<HttpSecurity> session) {
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    /**
     * Configures CORS to allow frontend communication from different origins.
     * Parses allowed origins from configuration, enables credentials,
     * and allows all HTTP methods and headers.
     * @return configured CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Parse allowed origins from configuration (comma-separated list)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
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
     * Creates a BCrypt password encoder with configured strength.
     * Strength must be between 4 and 31.
     * @return configured BCryptPasswordEncoder
     * @throws IllegalArgumentException if strength is out of valid range
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        if (bcryptStrength < 4 || bcryptStrength > 31)
            throw new IllegalArgumentException("BCrypt strength must be between 4 and 31");
        return new BCryptPasswordEncoder(bcryptStrength);
    }

    /**
     * Creates authentication entry point that handles unauthorized access (401).
     * Returns JSON error response when authentication fails or token is invalid.
     * @return configured AuthenticationEntryPoint
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            Map<String, Object> errorDetails = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 401,
                    "error", "Unauthorized",
                    "message", authException.getMessage(),
                    "path", request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
        };
    }

    /**
     * Creates access denied handler for forbidden access (403).
     * Returns JSON error response when user lacks required permissions.
     * @return configured AccessDeniedHandler
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            Map<String, Object> errorDetails = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 403,
                    "error", "Forbidden",
                    "message", accessDeniedException.getMessage(),
                    "path", request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
        };
    }

}