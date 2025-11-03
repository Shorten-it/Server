package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.domain.Url;
import com.url.ShortenIt.domain.url.repository.Urlrepository;
import com.url.ShortenIt.domain.url.util.BaseConversion;
import com.url.ShortenIt.domain.url.util.SnowFlake;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

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
    public String saveShortUrl(String longUrl) {
        // 1) 캐시에서 먼저 조회
        String cachedShort = getFromCache(CACHE_L2S_PREFIX + longUrl);
        if (cachedShort != null) {
            return cachedShort;
        }

        Optional<Url> url = Urlrepository.findByLongUrl(longUrl);
        if (url.isPresent()) {
            String shortUrl = url.get().getShortUrl();
            // 캐시에 기록
            putToCache(CACHE_L2S_PREFIX + longUrl, shortUrl);
            putToCache(CACHE_S2L_PREFIX + shortUrl, longUrl);
            return shortUrl;
        }else{
            SnowFlake snowFlake = new SnowFlake(1,1);
            Long id = snowFlake.nextId();
            String str = BaseConversion.encode(id);
            Url urlEntity = Url.create(longUrl,str);
            Urlrepository.save(urlEntity);
            // 캐시에 기록
            putToCache(CACHE_L2S_PREFIX + longUrl, str);
            putToCache(CACHE_S2L_PREFIX + str, longUrl);
            return urlEntity.getShortUrl();
        }

 

    }

    @Override
    public Url searchLongUrl(String shortUrl) {
        // 1) 캐시에서 조회
        String longUrl = getFromCache(CACHE_S2L_PREFIX + shortUrl);
        if (longUrl != null) {
            return Url.create(longUrl, shortUrl);
        }

        Optional<Url> url =  Urlrepository.findByShortUrl(shortUrl);
        Url found = url.get();
        // 캐시에 기록
        putToCache(CACHE_S2L_PREFIX + shortUrl, found.getLongUrl());
        putToCache(CACHE_L2S_PREFIX + found.getLongUrl(), shortUrl);
        return found;
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