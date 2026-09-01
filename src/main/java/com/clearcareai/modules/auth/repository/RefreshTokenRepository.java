package com.clearcareai.modules.auth.repository;

import com.clearcareai.modules.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // the client holds the token string, never the row id
    Optional<RefreshToken> findByToken(String token);
    int deleteByExpiryDateBefore(LocalDateTime dateTime);
}