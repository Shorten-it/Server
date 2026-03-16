package com.url.ShortenIt.domain.url.scheduler;

import com.url.ShortenIt.domain.url.repository.Urlrepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredUrlCleanupScheduler {

    private final Urlrepository urlrepository;

    @Scheduled(cron = "0 0 * * * *") // 매시간 정각에 실행
    @Transactional
    public void cleanupExpiredUrls() {
        Instant now = Instant.now();
        int deletedCount = urlrepository.deleteAllExpiredBefore(now);
        if (deletedCount > 0) {
            log.info("Expired URL cleanup: {} URLs deleted", deletedCount);
        }
    }
}
