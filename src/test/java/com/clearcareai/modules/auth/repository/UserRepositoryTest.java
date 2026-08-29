package com.clearcareai.modules.auth.repository;

import com.clearcareai.modules.auth.entity.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // Helper method to create a User
    private User buildUser(String email, User.Role role) {

        return User.builder()
                .email(email)
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .firstName("Rahul")
                .lastName("Sharma")
                .phone("9876543210")
                .role(role)
                .build();
    }

    // --------------------------------------------------
    // 1. Save and find user by email
    // --------------------------------------------------

    @Test
    void save_thenFindByEmail_returnsTheUser() {

        User user = buildUser(
                "rahul@example.com",
                User.Role.ROLE_PATIENT
        );

        userRepository.save(user);

        Optional<User> found =
                userRepository.findByEmail("rahul@example.com");

        assertTrue(found.isPresent());

        User savedUser = found.get();

        assertEquals(
                "rahul@example.com",
                savedUser.getEmail()
        );

        assertEquals(
                "Rahul",
                savedUser.getFirstName()
        );

        assertEquals(
                User.Role.ROLE_PATIENT,
                savedUser.getRole()
        );

        assertNotNull(savedUser.getId());

        assertNotNull(savedUser.getCreatedAt());
    }

    // --------------------------------------------------
    // 2. Find by email - email does not exist
    // --------------------------------------------------

    @Test
    void findByEmail_returnsEmptyWhenNobodyHasThatEmail() {

        Optional<User> found =
                userRepository.findByEmail(
                        "nobody@example.com"
                );

        assertFalse(found.isPresent());
    }

    // --------------------------------------------------
    // 3. isActive defaults to true
    // --------------------------------------------------

    @Test
    void save_withoutSettingIsActive_defaultsToTrue() {

        User user = buildUser(
                "default@example.com",
                User.Role.ROLE_PATIENT
        );

        User savedUser =
                userRepository.save(user);

        assertEquals(
                Boolean.TRUE,
                savedUser.getIsActive()
        );
    }

    // --------------------------------------------------
    // 4. existsByEmail
    // --------------------------------------------------

    @Test
    void existsByEmail_returnsTrueOnlyForARegisteredEmail() {

        User user = buildUser(
                "taken@example.com",
                User.Role.ROLE_DOCTOR
        );

        userRepository.save(user);

        assertTrue(
                userRepository.existsByEmail(
                        "taken@example.com"
                )
        );

        assertFalse(
                userRepository.existsByEmail(
                        "free@example.com"
                )
        );
    }

    // --------------------------------------------------
    // 5. Duplicate email
    // --------------------------------------------------

    @Test
    void save_withDuplicateEmail_throws() {

        User firstUser = buildUser(
                "same@example.com",
                User.Role.ROLE_PATIENT
        );

        userRepository.save(firstUser);

        User duplicateUser = buildUser(
                "same@example.com",
                User.Role.ROLE_DOCTOR
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(
                        duplicateUser
                )
        );
    }

    // --------------------------------------------------
    // 6. Count users by role
    // --------------------------------------------------

    @Test
    void countByRole_countsOnlyThatRole() {

        userRepository.save(
                buildUser(
                        "d1@example.com",
                        User.Role.ROLE_DOCTOR
                )
        );

        userRepository.save(
                buildUser(
                        "d2@example.com",
                        User.Role.ROLE_DOCTOR
                )
        );

        userRepository.save(
                buildUser(
                        "p1@example.com",
                        User.Role.ROLE_PATIENT
                )
        );

        assertEquals(
                2,
                userRepository.countByRole(
                        User.Role.ROLE_DOCTOR
                )
        );

        assertEquals(
                1,
                userRepository.countByRole(
                        User.Role.ROLE_PATIENT
                )
        );

        assertEquals(
                0,
                userRepository.countByRole(
                        User.Role.ROLE_ADMIN
                )
        );
    }

    // --------------------------------------------------
    // 7. Find users by role with pagination
    // --------------------------------------------------

    @Test
    void findByRole_paginates() {

        // Save 3 doctors

        userRepository.save(
                buildUser(
                        "d1@example.com",
                        User.Role.ROLE_DOCTOR
                )
        );

        userRepository.save(
                buildUser(
                        "d2@example.com",
                        User.Role.ROLE_DOCTOR
                )
        );

        userRepository.save(
                buildUser(
                        "d3@example.com",
                        User.Role.ROLE_DOCTOR
                )
        );

        // Page number = 0
        // Page size = 2

        Pageable firstPage =
                PageRequest.of(0, 2);

        // Find doctors with pagination

        Page<User> page =
                userRepository.findByRole(
                        User.Role.ROLE_DOCTOR,
                        firstPage
                );

        // Current page contains 2 doctors

        assertEquals(
                2,
                page.getContent().size()
        );

        // Total doctors = 3

        assertEquals(
                3,
                page.getTotalElements()
        );

        // 3 doctors with 2 per page = 2 pages

        assertEquals(
                2,
                page.getTotalPages()
        );

        // Page 0 is not the last page

        assertFalse(
                page.isLast()
        );
    }
}