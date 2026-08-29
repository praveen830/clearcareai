package com.clearcareai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    // 59 characters, comfortably over the 32-character floor
    private static final String SECRET = "myDefaultSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm";

    private JwtTokenProvider jwtTokenProvider;

    // builds a properties object by hand - no Spring needed, because the
    // provider takes its config through the constructor (this is D9 paying off)
    private JwtProperties buildProperties(String secret, Long expiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setExpiration(expiration);
        properties.setRefreshExpiration(604800000L);
        return properties;
    }

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(buildProperties(SECRET, 86400000L));
    }

    @Test
    void generateAccessToken_producesAThreePartToken() {
        String token = jwtTokenProvider.generateAccessToken("rahul@example.com", 1L, "ROLE_PATIENT");

        String[] parts = token.split("\\.");

        // header . payload . signature
        assertEquals(3, parts.length);
    }

    @Test
    void generatedToken_carriesEmailUserIdAndRole() {
        String token = jwtTokenProvider.generateAccessToken("rahul@example.com", 7L, "ROLE_DOCTOR");

        assertEquals("rahul@example.com", jwtTokenProvider.getEmailFromToken(token));
        assertEquals(7L, jwtTokenProvider.getUserIdFromToken(token));
        assertEquals("ROLE_DOCTOR", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_returnsTrueForAFreshToken() {
        String token = jwtTokenProvider.generateAccessToken("rahul@example.com", 1L, "ROLE_PATIENT");

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForATokenSignedWithADifferentSecret() {
        // a second provider with a different secret - this is the forgery case
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                buildProperties("aCompletelyDifferentSecretKeyThatIsAlsoLongEnough", 86400000L));

        String foreignToken = otherProvider.generateAccessToken("attacker@example.com", 99L, "ROLE_ADMIN");

        // structurally perfect, correctly signed - just not by US
        assertFalse(jwtTokenProvider.validateToken(foreignToken));
    }

    @Test
    void validateToken_returnsFalseForGarbage() {
        assertFalse(jwtTokenProvider.validateToken("this.is.not-a-jwt"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void validateToken_returnsFalseForAnExpiredToken() throws InterruptedException {
        // a provider whose tokens live for one millisecond
        JwtTokenProvider shortLived = new JwtTokenProvider(buildProperties(SECRET, 1L));

        String token = shortLived.generateAccessToken("rahul@example.com", 1L, "ROLE_PATIENT");

        // just long enough for exp to be in the past
        Thread.sleep(50);

        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void payloadIsReadableWithoutTheSecret() {
        String token = jwtTokenProvider.generateAccessToken("rahul@example.com", 1L, "ROLE_PATIENT");

        // decode the middle section with no key at all
        String[] parts = token.split("\\.");
        byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
        String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);

        // a JWT is SIGNED, not ENCRYPTED - anyone holding it can read every claim
        assertTrue(payloadJson.contains("rahul@example.com"));
        assertTrue(payloadJson.contains("ROLE_PATIENT"));

        // which is exactly why no password, hash or personal data goes in a claim
    }

    @Test
    void getRefreshExpirationMs_returnsTheConfiguredValue() {
        assertEquals(604800000L, jwtTokenProvider.getRefreshExpirationMs());
    }
}