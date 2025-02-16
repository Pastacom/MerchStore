package org.pastacom.MerchStore.util;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpirationMs;

    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(String username) {
        return generateJwt(username, jwtExpirationMs);
    }

    public String generateRefreshToken(String username) {
        return generateJwt(username, refreshExpirationMs);
    }

    private String generateJwt(String username, long expiration) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateJwtToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException
                 | SignatureException | IllegalArgumentException e) {
            return false;
        }
    }
}
