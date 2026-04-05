package com.url.ShortenIt.redirectservice.service;

import com.url.ShortenIt.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.url.ShortenIt.common.service.CacheService.CACHE_L2S_PREFIX;
import static com.url.ShortenIt.common.service.CacheService.CACHE_S2L_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber {

    private final CacheService cacheService;

    public void onMessage(String message, String channel) {
        try {
            String[] parts = message.split("\\|", 2);
            if (parts.length != 2) {
                log.warn("Invalid cache invalidation message: {}", message);
                return;
            }
            String shortUrl = parts[0];
            String longUrl = parts[1];

            cacheService.delete(CACHE_S2L_PREFIX + shortUrl);
            cacheService.delete(CACHE_L2S_PREFIX + longUrl);
            log.info("Cache invalidated for shortUrl={}, longUrl={}", shortUrl, longUrl);
        } catch (Exception e) {
            log.error("Failed to process cache invalidation message", e);
        }
    }
}
