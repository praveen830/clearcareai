package com.clearcareai.modules.auth.repository;

import com.clearcareai.modules.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// no @Repository needed - Spring Data registers a proxy for this interface
// automatically, and applies exception translation itself
public interface UserRepository extends JpaRepository<User, Long> {

    // used at login and wherever we resolve the logged-in user
    Optional<User> findByEmail(String email);

    // used by register - only needs a yes/no, so it never builds a User
    boolean existsByEmail(String email);

    // used by the admin dashboard in B6
    long countByRole(User.Role role);

    // used by the admin user list in B6
    Page<User> findByRole(User.Role role, Pageable pageable);
}