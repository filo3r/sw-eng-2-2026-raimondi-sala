package it.polimi.se.bbp.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    void testGenerateAndValidateToken() {
        Long userId = 123L;
        
        // generate
        String token = jwtService.generateToken(userId);
        assertNotNull(token);

        // extract
        Long extractedId = jwtService.extractUserId(token);
        assertEquals(userId, extractedId);

        // validate
        assertTrue(jwtService.isTokenValid(token, userId));
        assertFalse(jwtService.isTokenValid(token, 999L));
    }
}