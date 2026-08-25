package com.clearcareai.moduels.auth.repository;

import java.util.Optional;


import org.springdoc.core.converters.models.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.clearcareai.moduels.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(User.Role role);
    Page<User> findByRole(User.Role role, Pageable pageable);


} 