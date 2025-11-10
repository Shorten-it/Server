package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.domain.Url;
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

@AllArgsConstructor
@Service
public class UrlServiceIpml implements UrlService {

    private final Urlrepository Urlrepository;
    private final BaseConversion BaseConversion;
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_L2S_PREFIX = "L2S:"; // long -> short
    private static final String CACHE_S2L_PREFIX = "S2L:"; // short -> long

    @Override
    @Transactional
    public String saveShortUrl(String longUrl) {
        String normalized = normalizeLongUrl(longUrl);

        // 1. 캐시 조회 (Cache-Aside 패턴 적용)
        String cachedShort = getFromCache(CACHE_L2S_PREFIX + normalized);
        if (cachedShort != null) {
            return cachedShort;
        }

        // 2. DB 조회
        Optional<Url> url = Urlrepository.findByLongUrl(normalized);
        if (url.isPresent()) {
            String shortUrl = url.get().getShortUrl();
            // 캐시 미스
            putToCache(CACHE_L2S_PREFIX + normalized, shortUrl);
            putToCache(CACHE_S2L_PREFIX + shortUrl, normalized);
            return shortUrl;
        }else{
            // 4. 신규 URL 생성 (SnowFlake + Base62
            SnowFlake snowFlake = new SnowFlake(1,1);
            Long id = snowFlake.nextId();
            String str = BaseConversion.encode(id);
            Url urlEntity = Url.create(normalized,str);
            Urlrepository.save(urlEntity);
            // 캐시에 기록
            putToCache(CACHE_L2S_PREFIX + normalized, str);
            putToCache(CACHE_S2L_PREFIX + str, normalized);
            return urlEntity.getShortUrl();
        }

 

    }

    @Override
    @Transactional(readOnly = true)
    public Url searchLongUrl(String shortUrl) {
        // 1) 캐시에서 조회
        String longUrl = getFromCache(CACHE_S2L_PREFIX + shortUrl);
        if (longUrl != null) {
            return Url.create(longUrl, shortUrl);
        }

        Optional<Url> url =  Urlrepository.findByShortUrl(shortUrl);
        Url found = url.orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));
        // 캐시에 기록
        putToCache(CACHE_S2L_PREFIX + shortUrl, found.getLongUrl());
        putToCache(CACHE_L2S_PREFIX + found.getLongUrl(), shortUrl);
        return found;
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
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception ignored) {
        }
    }
        
}
