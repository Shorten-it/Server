package com.url.ShortenIt.urlservice.service;

import com.url.ShortenIt.common.domain.Url;
import com.url.ShortenIt.common.dto.response.ShortUrlResponse;
import com.url.ShortenIt.common.repository.Urlrepository;
import com.url.ShortenIt.common.util.BaseConversion;
import com.url.ShortenIt.common.util.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UrlManagementServiceImpl implements UrlManagementService {

    private final Urlrepository urlrepository;
    private final BaseConversion baseConversion;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final SnowFlake snowFlake;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_L2S_PREFIX = "L2S:";
    private static final String CACHE_S2L_PREFIX = "S2L:";

    @Override
    @Transactional
    public ShortUrlResponse saveShortUrl(String longUrl, Instant expiredAt) {
        String normalized = normalizeLongUrl(longUrl);

        String cachedShort = getFromCache(CACHE_L2S_PREFIX + normalized);
        if (cachedShort != null) {
            return new ShortUrlResponse(cachedShort);
        }

        Optional<Url> url = urlrepository.findByLongUrl(normalized);
        if (url.isPresent()) {
            String shortUrl = url.get().getShortUrl();
            putToCache(CACHE_L2S_PREFIX + normalized, shortUrl);
            putToCache(CACHE_S2L_PREFIX + shortUrl, normalized);
            return new ShortUrlResponse(shortUrl);
        } else {
            Long id = snowFlake.nextId();
            String str = baseConversion.encode(id);
            Url urlEntity = Url.create(normalized, str, expiredAt);
            urlrepository.save(urlEntity);
            Duration cacheTtl = calculateCacheTtl(expiredAt);
            putToCache(CACHE_L2S_PREFIX + normalized, str, cacheTtl);
            putToCache(CACHE_S2L_PREFIX + str, normalized, cacheTtl);
            return new ShortUrlResponse(urlEntity.getShortUrl());
        }
    }

    @Override
    @Transactional
    public void deleteUrl(String shortUrl) {
        Url url = urlrepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));
        String longUrl = url.getLongUrl();
        urlrepository.delete(url);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheInvalidationPublisher.publishInvalidation(shortUrl, longUrl);
            }
        });
    }

    private String normalizeLongUrl(String original) {
        if (original == null) {
            throw new IllegalArgumentException("long_url must not be null");
        }
        String trimmed = original.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://" + trimmed;
        }
        try {
            URI uri = new URI(trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("Invalid URL: " + original);
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + original);
        }
        return trimmed;
    }

    private String getFromCache(String key) {
        if (redisTemplate == null) return null;
        try { return redisTemplate.opsForValue().get(key); }
        catch (Exception ignored) { return null; }
    }

    private void putToCache(String key, String value) {
        putToCache(key, value, Duration.ofHours(24));
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
