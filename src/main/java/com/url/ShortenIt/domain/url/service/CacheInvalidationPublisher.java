package com.url.ShortenIt.domain.url.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationPublisher {

    private static final String CACHE_INVALIDATION_TOPIC = "cache:invalidation";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void publishInvalidation(String shortUrl, String longUrl) {
        if (redisTemplate == null) return;
        try {
            String message = shortUrl + "|" + longUrl;
            redisTemplate.convertAndSend(CACHE_INVALIDATION_TOPIC, message);
        } catch (Exception ignored) {
        }
    }
}
