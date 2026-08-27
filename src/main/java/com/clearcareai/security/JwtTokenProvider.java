package com.clearcareai.security;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtProperties jwtProperties;
    private SecretKey getSigningKey(){
        byte[] keyByte=jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyByte);

    }
        public String generateAcessToken(String email,Long userId,String role){
            Date now =new Date();
            Date expiry= new Date(now.getTime()+jwtProperties.getExpiration());
            return Jwts.builder().subject(email).claim("userId", userId).claim("role", role).issuedAt(now).expiration(expiry).signWith(getSigningKey(),Jwts.SIG.HS256).compact();

        }
        public long getRefreshExpirationMs(){
             return jwtProperties.getRefreshExpiration();

        }
        public String getEmailFromToken(String token){
            return parseClaims(token).getSubject();

        }
        public String getUserFromToken(String token){
            return parseClaims(token).get("userId",String.class);

        }
        public String getRoleFromToken(String token){
            return parseClaims(token).get("role",String.class);
        }
        private Claims parseClaims(String token ){
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
          return false;
    }
}


    
    

