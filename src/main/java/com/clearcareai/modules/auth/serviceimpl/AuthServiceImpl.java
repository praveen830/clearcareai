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
import com.clearcareai.modules.auth.service.AuthService;
import com.clearcareai.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // "Bearer" with NO trailing space - AppConstants.TOKEN_PREFIX is "Bearer "
    // WITH one, because that is stripped from a header. Two jobs, two constants
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // this check is for the error message; the unique constraint on
        // users.email is the actual guarantee under concurrency
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))   // never the raw value
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                // safe because @Pattern already restricted the field to 3 values
                .role(User.Role.valueOf(request.getRole()))
                .isActive(true)
                .build();

        // reassign: the returned instance has the generated id and createdAt
        user = userRepository.save(user);
        log.info("Registered new user with email: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        try {
            // TWO-argument constructor: an UNAUTHENTICATED token, a claim to check.
            // Part 5's filter used the three-argument one for the opposite reason
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            // identical whether the email is unknown or the password is wrong -
            // this is what prevents user enumeration
            throw new UnauthorizedException("Invalid email or password");
        } catch (DisabledException ex) {
            // safe to be specific: only reaches someone who knew the password
            throw new UnauthorizedException("Your account has been deactivated. Contact the administrator.");
        }

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (!userOptional.isPresent()) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userOptional.get();
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        Optional<RefreshToken> tokenOptional = refreshTokenRepository.findByToken(request.getRefreshToken());

        if (!tokenOptional.isPresent()) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        RefreshToken existingToken = tokenOptional.get();

        // null-safe: a null isRevoked reads as "not revoked", which is why
        // @Builder.Default matters on that field
        if (Boolean.TRUE.equals(existingToken.getIsRevoked())) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (existingToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        // rotation: the token we came in with can never be used again
        existingToken.setIsRevoked(true);
        refreshTokenRepository.save(existingToken);

        // the lazy load from Part 2 fires here - fine, we are inside @Transactional
        User user = existingToken.getUser();
        return buildAuthResponse(user);
    }

    // all three public methods end here, which is why all three return the same shape
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    private String createRefreshToken(User user) {

        // an opaque random value, NOT a JWT - it has to be looked up anyway,
        // so being self-describing would buy nothing and leak claims
        String token = UUID.randomUUID().toString();

        // the config value is in MILLISECONDS; plusSeconds wants seconds.
        // Drop the / 1000 and the token expires in the year 21164, silently
        long refreshExpirationSeconds = jwtTokenProvider.getRefreshExpirationMs() / 1000;
        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(refreshExpirationSeconds);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}