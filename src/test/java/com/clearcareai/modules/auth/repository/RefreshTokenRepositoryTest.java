package com.clearcareai.modules.auth.repository;

import com.clearcareai.modules.auth.entity.RefreshToken;
import com.clearcareai.modules.auth.entity.User;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    // lets us flush and clear the persistence context, which the lazy test needs
    @Autowired
    private TestEntityManager entityManager;

    // a saved user to hang tokens off - the token's user must already exist,
    // because there is no cascade on the relationship
    private User saveUser(String email) {
        User user = User.builder()
                .email(email)
                .password("$2a$10$abcdefghijklmnopqrstuv")
                .firstName("Rahul")
                .lastName("Sharma")
                .phone("9876543210")
                .role(User.Role.ROLE_PATIENT)
                .build();

        return userRepository.save(user);
    }

    private RefreshToken buildToken(User user, String tokenValue) {
        return RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void save_thenFindByToken_returnsTheTokenWithItsUser() {
        User user = saveUser("rahul@example.com");
        refreshTokenRepository.save(buildToken(user, "token-abc"));

        Optional<RefreshToken> found = refreshTokenRepository.findByToken("token-abc");

        assertTrue(found.isPresent());

        RefreshToken savedToken = found.get();
        assertEquals("token-abc", savedToken.getToken());
        assertNotNull(savedToken.getId());
        assertNotNull(savedToken.getCreatedAt());

        // the relationship resolves back to the right user
        assertEquals("rahul@example.com", savedToken.getUser().getEmail());
    }

    @Test
    void findByToken_returnsEmptyWhenTokenIsUnknown() {
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("no-such-token");

        // this is the path a forged or stale token takes - Part 6 turns it into a 401
        assertFalse(found.isPresent());
    }

    @Test
    void save_withoutSettingIsRevoked_defaultsToFalse() {
        User user = saveUser("default@example.com");

        // notice we never call .isRevoked(...) on the builder
        RefreshToken savedToken = refreshTokenRepository.save(buildToken(user, "token-default"));

        // null here means @Builder.Default is missing, and a revoked token
        // would later read as still active
        assertEquals(Boolean.FALSE, savedToken.getIsRevoked());
    }

    @Test
    void save_withDuplicateToken_throws() {
        User user = saveUser("dup@example.com");
        refreshTokenRepository.save(buildToken(user, "same-token"));

        RefreshToken duplicate = buildToken(user, "same-token");

        // saveAndFlush so the INSERT actually reaches the database
        assertThrows(DataIntegrityViolationException.class, () -> {
            refreshTokenRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    void save_withoutUser_throws() {
        RefreshToken orphan = RefreshToken.builder()
                .token("orphan-token")
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        // nullable = false on the join column - Hibernate rejects this before
        // any SQL is sent
        assertThrows(DataIntegrityViolationException.class, () -> {
            refreshTokenRepository.saveAndFlush(orphan);
        });
    }

    @Test
    void oneUser_canHaveManyRefreshTokens() {
        User user = saveUser("many@example.com");

        // same person, three devices
        refreshTokenRepository.save(buildToken(user, "laptop-token"));
        refreshTokenRepository.save(buildToken(user, "phone-token"));
        refreshTokenRepository.save(buildToken(user, "tablet-token"));

        assertEquals(3, refreshTokenRepository.count());
        assertTrue(refreshTokenRepository.findByToken("phone-token").isPresent());
    }

    @Test
    void userAssociation_isLazy_untilItIsTouched() {
        User user = saveUser("lazy@example.com");
        refreshTokenRepository.save(buildToken(user, "lazy-token"));

        // push everything to the database and empty the persistence context,
        // so the next read is a real load and not a cache hit
        entityManager.flush();
        entityManager.clear();

        RefreshToken loadedToken = refreshTokenRepository.findByToken("lazy-token").get();

        // the user field holds a proxy, not a loaded User - no query has run for it
        assertFalse(Hibernate.isInitialized(loadedToken.getUser()));

        // touching it fires the SELECT
        String email = loadedToken.getUser().getEmail();

        assertEquals("lazy@example.com", email);
        assertTrue(Hibernate.isInitialized(loadedToken.getUser()));
    }
}