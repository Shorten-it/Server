package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.domain.Url;
import com.url.ShortenIt.domain.url.dto.response.ShortUrlResponse;
import com.url.ShortenIt.domain.url.dto.response.UrlInfoResponse;
import com.url.ShortenIt.domain.url.repository.Urlrepository;
import com.url.ShortenIt.domain.url.util.BaseConversion;
import com.url.ShortenIt.domain.url.util.SnowFlake;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;

@AllArgsConstructor
@Service
public class UrlServiceIpml implements UrlService {

    private final Urlrepository Urlrepository;
    private final BaseConversion BaseConversion;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_L2S_PREFIX = "L2S:"; // long -> short
    private static final String CACHE_S2L_PREFIX = "S2L:"; // short -> long

    @Override
    @Transactional
    public ShortUrlResponse saveShortUrl(String longUrl, Instant expiredAt) {
        String normalized = normalizeLongUrl(longUrl);

        // 1. 캐시 조회 (Cache-Aside 패턴 적용)
        String cachedShort = getFromCache(CACHE_L2S_PREFIX + normalized);
        if (cachedShort != null) {
            return new ShortUrlResponse(cachedShort);
        }

        // 2. DB 조회
        Optional<Url> url = Urlrepository.findByLongUrl(normalized);
        if (url.isPresent()) {
            String shortUrl = url.get().getShortUrl();
            // 캐시 미스
            putToCache(CACHE_L2S_PREFIX + normalized, shortUrl);
            putToCache(CACHE_S2L_PREFIX + shortUrl, normalized);
            return new ShortUrlResponse(shortUrl);
        }else{
            // 4. 신규 URL 생성 (SnowFlake + Base62)
            SnowFlake snowFlake = new SnowFlake(1,1);
            Long id = snowFlake.nextId();
            String str = BaseConversion.encode(id);
            Url urlEntity = Url.create(normalized, str, expiredAt);
            Urlrepository.save(urlEntity);
            // 캐시에 기록
            putToCache(CACHE_L2S_PREFIX + normalized, str);
            putToCache(CACHE_S2L_PREFIX + str, normalized);
            return new ShortUrlResponse(urlEntity.getShortUrl());
        }

 

    }

    @Override
    @Transactional(readOnly = true)
    public UrlInfoResponse searchLongUrl(String shortUrl) {
        // 1) 캐시에서 조회
        String longUrl = getFromCache(CACHE_S2L_PREFIX + shortUrl);
        if (longUrl != null) {
            return new UrlInfoResponse(longUrl, shortUrl);
        }

        Optional<Url> url =  Urlrepository.findByShortUrl(shortUrl);
        Url found = url.orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));
        // 만료된 URL 체크
        if (found.isExpired()) {
            throw new IllegalArgumentException("This URL has expired: " + shortUrl);
        }
        // 캐시에 기록
        putToCache(CACHE_S2L_PREFIX + shortUrl, found.getLongUrl());
        putToCache(CACHE_L2S_PREFIX + found.getLongUrl(), shortUrl);
        return new UrlInfoResponse(found.getLongUrl(), found.getShortUrl());
    }

    @Override
    @Transactional
    public void deleteUrl(String shortUrl) {
        Url url = Urlrepository.findByShortUrl(shortUrl).orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));
        String longUrl = url.getLongUrl();
        Urlrepository.delete(url);
        // Redis Pub/Sub을 통한 캐시 무효화 (모든 인스턴스에 전파)
        cacheInvalidationPublisher.publishInvalidation(shortUrl, longUrl);
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
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void putToCache(String key, String value) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofHours(24));
        } catch (Exception ignored) {
        }
    }

    private void deleteFromCache(String key) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }
        
}
