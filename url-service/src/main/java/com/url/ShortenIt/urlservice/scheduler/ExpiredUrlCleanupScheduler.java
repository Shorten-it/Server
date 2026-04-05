package com.url.ShortenIt.urlservice.scheduler;

import com.url.ShortenIt.common.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredUrlCleanupScheduler {

    private static final String LOCK_KEY = "lock:expired-url-cleanup";
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);

    private final UrlRepository urlRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredUrls() {
        if (!tryLock()) {
            log.debug("Another instance is running expired URL cleanup. Skipping.");
            return;
        }
        try {
            Instant now = Instant.now();
            int deletedCount = urlRepository.softDeleteAllExpiredBefore(now);
            if (deletedCount > 0) {
                log.info("Expired URL cleanup: {} URLs soft-deleted", deletedCount);
            }
        } finally {
            releaseLock();
        }
    }

    private boolean tryLock() {
        if (redisTemplate == null) return true;
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_KEY, "locked", LOCK_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Failed to acquire distributed lock: {}", e.getMessage());
            return true;
        }
    }

    private void releaseLock() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(LOCK_KEY);
        } catch (Exception e) {
            log.warn("Failed to release distributed lock: {}", e.getMessage());
        }
    }
}
