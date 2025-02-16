package org.pastacom.MerchStore.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private String secret = "longlongtestjwtohiosecretisstoredrightthere";
    private long accessExpirationMs = 10000;
    private long refreshExpirationMs = 20000;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(jwtUtil, "secret", secret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", accessExpirationMs);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpirationMs", refreshExpirationMs);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtUtil.generateToken("testUser");
        assertNotNull(token);
        assertEquals("testUser", jwtUtil.extractClaim(token, Claims::getSubject));
    }

    @Test
    void extractExpiration_shouldReturnCorrectExpiration() {
        String token = jwtUtil.generateToken("testUser");
        Instant expiration = jwtUtil.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));

        String refreshToken = jwtUtil.generateRefreshToken("testUser");
        expiration = jwtUtil.extractExpiration(refreshToken);
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));
    }

    @Test
    void validateJwtToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken("testUser");
        assertTrue(jwtUtil.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_shouldReturnFalseForExpiredToken() throws InterruptedException {
        String expiredToken = Jwts.builder()
                .setSubject("testUser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20000))
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        assertFalse(jwtUtil.validateJwtToken(expiredToken));
    }

    @Test
    void validateJwtToken_shouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.token.value";
        assertFalse(jwtUtil.validateJwtToken(invalidToken));
    }
}
