package io.github.michaelcommitsat3am.transactionfraudcomp.component.security;

public interface AuthService {
    /**
     * parses the token, verifies the signature, and ensures it belongs to the userId.
     * @throws SecurityException if invalid or expired.
     */
    void validateRequest(String token, String userId) throws SecurityException;
}