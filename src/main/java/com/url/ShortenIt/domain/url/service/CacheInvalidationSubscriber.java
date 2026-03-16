package com.url.ShortenIt.domain.url.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheInvalidationSubscriber {

    private static final String CACHE_L2S_PREFIX = "L2S:";
    private static final String CACHE_S2L_PREFIX = "S2L:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void onMessage(String message, String channel) {
        if (redisTemplate == null) return;
        try {
            String[] parts = message.split("\\|", 2);
            if (parts.length != 2) {
                log.warn("Invalid cache invalidation message: {}", message);
                return;
            }
            String shortUrl = parts[0];
            String longUrl = parts[1];

            redisTemplate.delete(CACHE_S2L_PREFIX + shortUrl);
            redisTemplate.delete(CACHE_L2S_PREFIX + longUrl);
            log.info("Cache invalidated for shortUrl={}, longUrl={}", shortUrl, longUrl);
        } catch (Exception e) {
            log.error("Failed to process cache invalidation message", e);
        }
    }
}
