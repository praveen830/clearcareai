package com.clearcareai.modules.auth.serviceimpl;

import com.clearcareai.exception.BadRequestException;
import com.clearcareai.exception.UnauthorizedException;
import com.clearcareai.modules.auth.dto.AuthResponse;
import com.clearcareai.modules.auth.dto.LoginRequest;
import com.clearcareai.modules.auth.dto.RefreshTokenRequest;
import com.clearcareai.modules.auth.dto.RegisterRequest;
import com.clearcareai.modules.auth.entity.RefreshToken;
import com.clearcareai.modules.auth.entity.User;
import com.clearcareai.modules.auth.repository.RefreshTokenRepository;
import com.clearcareai.modules.auth.repository.UserRepository;
import com.clearcareai.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("rahul@example.com");
        request.setPassword("Password123");
        request.setFirstName("Rahul");
        request.setLastName("Sharma");
        request.setPhone("9876543210");
        request.setRole("ROLE_PATIENT");
        return request;
    }

    private User buildSavedUser() {
        return User.builder()
                .id(1L)
                .email("rahul@example.com")
                .password("$2a$10$hashedValue")
                .firstName("Rahul")
                .lastName("Sharma")
                .phone("9876543210")
                .role(User.Role.ROLE_PATIENT)
                .isActive(true)
                .build();
    }

    // the two stubs every token-issuing path needs
    private void stubTokenGeneration() {
        when(jwtTokenProvider.generateAccessToken(anyString(), anyLong(), anyString()))
                .thenReturn("a.jwt.token");
        when(jwtTokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
    }

    @Test
    void register_encodesThePasswordAndSavesTheUser() {
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("$2a$10$hashedValue");
        when(userRepository.save(any(User.class))).thenReturn(buildSavedUser());
        stubTokenGeneration();

        AuthResponse response = authServiceImpl.register(buildRegisterRequest());

        // capture the User that was actually handed to save()
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // the raw password must never reach the database
        assertEquals("$2a$10$hashedValue", savedUser.getPassword());
        assertEquals(User.Role.ROLE_PATIENT, savedUser.getRole());
        assertEquals(Boolean.TRUE, savedUser.getIsActive());

        assertEquals("a.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1L, response.getUserId());
        assertEquals("ROLE_PATIENT", response.getRole());

        // a refresh row is written on registration too
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_withAnExistingEmail_throwsBadRequestAndSavesNothing() {
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            authServiceImpl.register(buildRegisterRequest());
        });

        assertEquals("Email is already registered: rahul@example.com", exception.getMessage());

        // it must bail out BEFORE writing anything
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_returnsBothTokens() {
        when(userRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(buildSavedUser()));
        stubTokenGeneration();

        LoginRequest request = new LoginRequest("rahul@example.com", "Password123");
        AuthResponse response = authServiceImpl.login(request);

        assertEquals("a.jwt.token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("rahul@example.com", response.getEmail());
    }

    @Test
    void login_withBadCredentials_doesNotSayWhichHalfWasWrong() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("rahul@example.com", "WrongPass123");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authServiceImpl.login(request);
        });

        // the SAME message an unknown email produces - this is what stops
        // user enumeration, so the exact text is worth pinning down
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void login_withADeactivatedAccount_saysSoExplicitly() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"));

        LoginRequest request = new LoginRequest("rahul@example.com", "Password123");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authServiceImpl.login(request);
        });

        // safe to be specific: this only reaches someone who knew the password
        assertEquals("Your account has been deactivated. Contact the administrator.",
                exception.getMessage());
    }

    @Test
    void refreshToken_revokesTheOldTokenAndIssuesANewPair() {
        RefreshToken existingToken = RefreshToken.builder()
                .id(5L)
                .user(buildSavedUser())
                .token("old-token")
                .expiryDate(LocalDateTime.now().plusDays(3))
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existingToken));
        stubTokenGeneration();

        AuthResponse response = authServiceImpl.refreshToken(new RefreshTokenRequest("old-token"));

        // rotation: the token we came in with is now dead
        assertTrue(existingToken.getIsRevoked());

        // and a brand new one was issued
        assertNotNull(response.getRefreshToken());
        assertEquals("a.jwt.token", response.getAccessToken());
    }

    @Test
    void refreshToken_withARevokedToken_throws() {
        RefreshToken revokedToken = RefreshToken.builder()
                .user(buildSavedUser())
                .token("used-token")
                .expiryDate(LocalDateTime.now().plusDays(3))
                .isRevoked(true)
                .build();

        when(refreshTokenRepository.findByToken("used-token")).thenReturn(Optional.of(revokedToken));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authServiceImpl.refreshToken(new RefreshTokenRequest("used-token"));
        });

        assertEquals("Refresh token has been revoked", exception.getMessage());
    }

    @Test
    void refreshToken_withAnExpiredToken_throws() {
        RefreshToken expiredToken = RefreshToken.builder()
                .user(buildSavedUser())
                .token("stale-token")
                .expiryDate(LocalDateTime.now().minusDays(1))
                .isRevoked(false)
                .build();

        when(refreshTokenRepository.findByToken("stale-token")).thenReturn(Optional.of(expiredToken));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authServiceImpl.refreshToken(new RefreshTokenRequest("stale-token"));
        });

        assertEquals("Refresh token has expired", exception.getMessage());
    }
}