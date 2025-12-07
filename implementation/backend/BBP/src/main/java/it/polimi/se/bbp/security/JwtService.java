package it.polimi.se.bbp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation.
 * Handles token creation, parsing, and validation operations.
 */
@Service
public class JwtService {

    /**
     * Secret key for signing JWT tokens.
     * Loaded from application.properties (jwt.secret).
     * Must be Base64-encoded string of at least 256 bits for HS256 algorithm.
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * JWT token expiration time in milliseconds.
     * Loaded from application.properties (jwt.expiration).
     * Default: 86400000 ms = 24 hours.
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extracts user ID (subject) from JWT token.
     * @param token JWT token
     * @return user ID extracted from token
     */
    public Long extractUserId(String token) {
        String userIdString = extractClaim(token, Claims::getSubject);
        return Long.parseLong(userIdString);
    }

    /**
     * Extracts specific claim from JWT token.
     * @param token JWT token
     * @param claimsResolver function to extract desired claim
     * @param <T> type of claim
     * @return extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates JWT token for given user ID.
     * @param userId user ID
     * @return generated JWT token
     */
    public String generateToken(Long userId) {
        return generateToken(new HashMap<>(), userId);
    }

    /**
     * Generates JWT token with additional claims.
     * @param extraClaims additional claims to include in token
     * @param userId user ID
     * @return generated JWT token
     */
    public String generateToken(Map<String, Object> extraClaims, Long userId) {
        return buildToken(extraClaims, userId, jwtExpiration);
    }

    /**
     * Builds JWT token with specified claims and expiration.
     * @param extraClaims additional claims
     * @param userId user ID
     * @param expiration expiration time in milliseconds
     * @return built JWT token
     */
    private String buildToken(Map<String, Object> extraClaims, Long userId, long expiration) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userId.toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Validates if token is valid for given user ID.
     * @param token JWT token
     * @param userId user ID
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid(String token, Long userId) {
        final Long tokenUserId = extractUserId(token);
        return tokenUserId.equals(userId) && !isTokenExpired(token);
    }

    /**
     * Checks if token is expired.
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts expiration date from token.
     * @param token JWT token
     * @return expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts all claims from token.
     * @param token JWT token
     * @return all claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets signing key for JWT token.
     * @return signing key
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}