package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.global.config.RedisPubSubConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheInvalidationPublisher {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void publishInvalidation(String shortUrl, String longUrl) {
        if (redisTemplate == null) return;
        try {
            String message = shortUrl + "|" + longUrl;
            redisTemplate.convertAndSend(RedisPubSubConfig.CACHE_INVALIDATION_TOPIC, message);
        } catch (Exception e) {
            log.error("Failed to publish cache invalidation message", e);
            throw new IllegalStateException("Failed to publish cache invalidation message", e);
        }
    }
}
