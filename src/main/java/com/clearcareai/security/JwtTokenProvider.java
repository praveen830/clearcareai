package com.clearcareai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // D9: the config arrives as one validated object, not three @Value fields.
    // There is deliberately no @Value anywhere in this class
    private final JwtProperties jwtProperties;

    // UTF_8 explicitly - the platform default differs between Windows and Linux,
    // which would produce a different key from the same secret
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // takes three plain values, not a User - this class knows nothing about entities
    public String generateAccessToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .subject(email)                 // becomes the "sub" claim
                .claim("userId", userId)        // custom claim
                .claim("role", role)            // custom claim
                .issuedAt(now)                  // "iat"
                .expiration(expiry)             // "exp" - JJWT checks this on parse
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    // used by AuthServiceImpl to work out a refresh token's expiry date
    public long getRefreshExpirationMs() {
        return jwtProperties.getRefreshExpiration();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // the two-arg get() matters: small JSON numbers come back as Integer,
    // so a plain cast to Long would throw ClassCastException
    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // the only place this class parses a token, and it always verifies
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SignatureException ex) {
            // forged or tampered with
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            // not a well-formed JWT at all
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            // the normal case - the client should now use its refresh token
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // null, empty or whitespace
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }

        // NOTE: there is deliberately no catch for UnsupportedJwtException.
        // The reference has a fifth block catching UnsupportedOperationException -
        // the JDK collections class, which JJWT can never throw - and we removed
        // it rather than correcting the type. An unsigned "alg":"none" token
        // therefore escapes to JwtAuthenticationFilter's blanket catch and ends
        // as the same 401. See section 1a (2)

        // every failure collapses to false - the caller only needs one bit,
        // and the client must never learn WHICH check failed
        return false;
    }
}