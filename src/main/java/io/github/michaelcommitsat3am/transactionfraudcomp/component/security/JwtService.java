package io.github.michaelcommitsat3am.transactionfraudcomp.component.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A mock implementation of JWT validation.
 * In a real scenario, this would parse and verify the JWT signature using a library like jjwt.
 */
public class JwtMockService implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(JwtMockService.class);

    @Override
    public void validateRequest(String token, String userId) {
        // PRODUCTION TODO: Actually verify JWT signature and claims
        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Security Alert: Missing or malformed token for user {}", userId);
            throw new SecurityException("Missing or malformed Authorization token");
        }

        // Mock logic: Token must contain the userId to be valid
        if (!token.contains(userId)) {
            logger.warn("Security Alert: Token owner mismatch. Token: {}, RequestUser: {}", token, userId);
            throw new SecurityException("Unauthorized: Token does not match requested user");
        }
    }
}