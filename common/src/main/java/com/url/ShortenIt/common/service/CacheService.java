package com.url.ShortenIt.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
public class CacheService {

    public static final String CACHE_L2S_PREFIX = "L2S:";
    public static final String CACHE_S2L_PREFIX = "S2L:";

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public String get(String key) {
        if (redisTemplate == null) return null;
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis GET failed for key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public void put(String key, String value) {
        put(key, value, DEFAULT_TTL);
    }

    public void put(String key, String value, Duration ttl) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Redis SET failed for key={}: {}", key, e.getMessage());
        }
    }

    public void delete(String key) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for key={}: {}", key, e.getMessage());
        }
    }

    public Duration calculateCacheTtl(Instant expiredAt) {
        if (expiredAt == null) return DEFAULT_TTL;
        Duration remaining = Duration.between(Instant.now(), expiredAt);
        if (remaining.isNegative() || remaining.isZero()) return Duration.ofSeconds(1);
        return remaining.compareTo(DEFAULT_TTL) < 0 ? remaining : DEFAULT_TTL;
    }
}
