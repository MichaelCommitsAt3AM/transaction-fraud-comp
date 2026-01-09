package io.github.michaelcommitsat3am.transactionfraudcomp.component.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtService implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final long tokenExpirationMs;

    // Default expiration: 1 hour
    private static final long DEFAULT_EXPIRATION_MS = 3600000;

    /**
     * Constructor with default 1-hour token expiration.
     */
    public JwtService(String secret) {
        this(secret, DEFAULT_EXPIRATION_MS);
    }

    /**
     * Constructor with configurable token expiration.
     * 
     * @param secret            JWT secret (must be at least 32 bytes for
     *                          HMAC-SHA256)
     * @param tokenExpirationMs Token lifetime in milliseconds
     */
    public JwtService(String secret, long tokenExpirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenExpirationMs = tokenExpirationMs;
    }

    /**
     * Generates a JWT token with expiration for the given user.
     */
    public String generateToken(String userId) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiration = new Date(nowMillis + tokenExpirationMs);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    @Override
    public void validateRequest(String tokenHeader, String userId) {
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header format");
            throw new SecurityException("Missing or invalid Authorization header");
        }

        String token = tokenHeader.substring(7); // Remove "Bearer " prefix

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();

            if (subject == null || subject.trim().isEmpty()) {
                logger.warn("Token has no subject");
                throw new SecurityException("Invalid token: missing subject");
            }

            if (!userId.equals(subject)) {
                logger.warn("Token subject mismatch: expected '{}', got '{}'", userId, subject);
                throw new SecurityException(
                        "Token subject ('" + subject + "') does not match User ID ('" + userId + "')");
            }

            // Check if token is expired (handled automatically by parser, but log it)
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                logger.warn("Token expired at {}", expiration);
            }

            logger.debug("Token validated successfully for user: {}", userId);

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            logger.warn("Expired JWT token for user: {}", userId);
            throw new SecurityException("Token has expired");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.warn("Invalid JWT signature for user: {}", userId);
            throw new SecurityException("Invalid token signature");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            logger.warn("Malformed JWT token for user: {}", userId);
            throw new SecurityException("Malformed token");
        } catch (Exception e) {
            logger.error("Token validation failed for user: {}", userId, e);
            throw new SecurityException("Invalid Token: " + e.getMessage());
        }
    }
}