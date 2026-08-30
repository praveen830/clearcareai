package com.clearcareai.security;

import com.clearcareai.modules.auth.entity.User;
import com.clearcareai.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    // a fake repository - no database involved at all
    @Mock
    private UserRepository userRepository;

    // Mockito builds the service and passes the mock into its constructor
    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User buildUser(User.Role role, Boolean isActive) {
        return User.builder()
                .id(1L)
                .email("rahul@example.com")
                .password("$2a$10$someBcryptHashValue")
                .firstName("Rahul")
                .lastName("Sharma")
                .phone("9876543210")
                .role(role)
                .isActive(isActive)
                .build();
    }

    @Test
    void loadUserByUsername_returnsUserDetailsWithThePrefixedRole() {
        User user = buildUser(User.Role.ROLE_DOCTOR, true);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("rahul@example.com");

        assertEquals("rahul@example.com", userDetails.getUsername());

        // the stored hash is passed through untouched - the AuthenticationManager
        // is what compares it
        assertEquals("$2a$10$someBcryptHashValue", userDetails.getPassword());

        // exactly one authority, and it KEEPS the ROLE_ prefix
        assertEquals(1, userDetails.getAuthorities().size());
        assertEquals("ROLE_DOCTOR",
                userDetails.getAuthorities().iterator().next().getAuthority());

        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_throwsWhenNobodyHasThatEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            customUserDetailsService.loadUserByUsername("nobody@example.com");
        });

        assertEquals("User not found with email: nobody@example.com", exception.getMessage());
    }

    @Test
    void anInactiveUser_comesBackDisabled() {
        User user = buildUser(User.Role.ROLE_PATIENT, false);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("rahul@example.com");

        // disabled is the inverse of isActive - this becomes DisabledException at login
        assertFalse(userDetails.isEnabled());
    }

    @Test
    void aNullIsActive_alsoComesBackDisabled_failClosed() {
        // this is the exact state Part 1's missing @Builder.Default would produce
        User user = buildUser(User.Role.ROLE_PATIENT, null);
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("rahul@example.com");

        // null must NOT mean "enabled" - a security default fails closed.
        // If this fails, the code used !user.getIsActive() and threw NPE, or
        // treated null as active
        assertFalse(userDetails.isEnabled());
    }
}