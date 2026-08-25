package com.clearcareai.moduels.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clearcareai.moduels.auth.entity.RefreshToken;

public interface RefershTokenRepository extends JpaRepository<RefreshToken, Long>{
    Optional<RefreshToken> findByToken(String token);
    
}

