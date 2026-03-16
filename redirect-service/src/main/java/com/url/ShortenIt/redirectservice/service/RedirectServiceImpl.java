package com.url.ShortenIt.redirectservice.service;

import com.url.ShortenIt.common.domain.Url;
import com.url.ShortenIt.common.dto.response.UrlInfoResponse;
import com.url.ShortenIt.common.repository.Urlrepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class RedirectServiceImpl implements RedirectService {

    private final Urlrepository urlrepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_L2S_PREFIX = "L2S:";
    private static final String CACHE_S2L_PREFIX = "S2L:";

    @Override
    @Transactional(readOnly = true)
    public UrlInfoResponse searchLongUrl(String shortUrl) {
        // 1) 캐시에서 조회
        String longUrl = getFromCache(CACHE_S2L_PREFIX + shortUrl);
        if (longUrl != null) {
            return new UrlInfoResponse(longUrl, shortUrl);
        }

        Optional<Url> url = urlrepository.findByShortUrl(shortUrl);
        Url found = url.orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));
        // 만료된 URL 체크
        if (found.isExpired()) {
            throw new IllegalStateException("This URL has expired: " + shortUrl);
        }
        // 캐시에 기록 (만료시간 기반 TTL)
        Duration cacheTtl = calculateCacheTtl(found.getExpiredAt());
        putToCache(CACHE_S2L_PREFIX + shortUrl, found.getLongUrl(), cacheTtl);
        putToCache(CACHE_L2S_PREFIX + found.getLongUrl(), shortUrl, cacheTtl);
        return new UrlInfoResponse(found.getLongUrl(), found.getShortUrl());
    }

    private String getFromCache(String key) {
        if (redisTemplate == null) return null;
        try { return redisTemplate.opsForValue().get(key); }
        catch (Exception ignored) { return null; }
    }

    private void putToCache(String key, String value, Duration ttl) {
        if (redisTemplate == null) return;
        try { redisTemplate.opsForValue().set(key, value, ttl); }
        catch (Exception ignored) {}
    }

    private Duration calculateCacheTtl(Instant expiredAt) {
        if (expiredAt == null) return Duration.ofHours(24);
        Duration remaining = Duration.between(Instant.now(), expiredAt);
        if (remaining.isNegative() || remaining.isZero()) return Duration.ofSeconds(1);
        Duration defaultTtl = Duration.ofHours(24);
        return remaining.compareTo(defaultTtl) < 0 ? remaining : defaultTtl;
    }
}
