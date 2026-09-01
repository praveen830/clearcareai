package com.clearcareai.modules.auth.scheduler;

import com.clearcareai.modules.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenCleanupScheduler refreshTokenCleanupScheduler;

    @Test
    void cleanup_deletesTokensThatExpiredBeforeNow() {
        when(refreshTokenRepository.deleteByExpiryDateBefore(any(LocalDateTime.class))).thenReturn(3);

        LocalDateTime beforeTheCall = LocalDateTime.now();
        refreshTokenCleanupScheduler.deleteExpiredRefreshTokens();
        LocalDateTime afterTheCall = LocalDateTime.now();

        // capture the cutoff that was actually used
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteByExpiryDateBefore(cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();

        // it must be "now", not a fixed date and not a date in the future -
        // a cutoff in the future would delete tokens that are still valid
        assertTrue(!cutoff.isBefore(beforeTheCall));
        assertTrue(!cutoff.isAfter(afterTheCall));
    }

    @Test
    void cleanup_survivesARepositoryFailure_soTomorrowsRunStillHappens() {
        when(refreshTokenRepository.deleteByExpiryDateBefore(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("database unavailable"));

        // it must NOT propagate: an exception escaping a @Scheduled method is
        // logged and forgotten, and with some schedules stops later runs
        assertDoesNotThrow(() -> refreshTokenCleanupScheduler.deleteExpiredRefreshTokens());
    }
}