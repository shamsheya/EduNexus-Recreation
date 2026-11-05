package com.edunexus.security;

import com.edunexus.user.Role;
import com.edunexus.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;

/**
 * Single JWT implementation (the reference had two incompatible ones).
 * Claims: sub = user id, email, roles (lowercase), iat, exp. HS256.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[64];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET is not set: using a random per-process signing key. Tokens will not survive a restart.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long");
            }
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expirationMs = expirationMs;
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        List<String> roles = user.roles.stream().map(Role::json).toList();
        return Jwts.builder()
                .subject(user.id)
                .claim("email", user.email)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** @return the user id (subject) of a valid token. @throws JwtException if invalid or expired. */
    public String validateAndGetUserId(String token) throws JwtException {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new JwtException("Token has no subject");
        }
        return sub;
    }
}
