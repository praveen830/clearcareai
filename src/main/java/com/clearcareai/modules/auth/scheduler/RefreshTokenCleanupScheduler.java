package com.clearcareai.modules.auth.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.clearcareai.modules.auth.repository.RefreshTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenRepository refreshTokenRepository;
    @Transactional
    @Scheduled(cron = "${app.auth.refresh-token-cleanup-cron:0 0 3 * * *}")
    public void deleteExpiredRefreshTokens(){
        try {
        LocalDateTime cutoff = LocalDateTime.now();

        int deletedCount =
                refreshTokenRepository.deleteByExpiryDateBefore(cutoff);

        if (deletedCount > 0) {
            log.info("Deleted {} expired refresh tokens", deletedCount);
        }
    } catch (Exception ex) {
        log.error("Refresh token cleanup failed: {}", ex.getMessage(), ex);
    }

    }
    
}
