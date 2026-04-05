package com.url.ShortenIt.redirectservice.service;

import com.url.ShortenIt.common.domain.Url;
import com.url.ShortenIt.common.dto.response.UrlInfoResponse;
import com.url.ShortenIt.common.repository.UrlRepository;
import com.url.ShortenIt.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

import static com.url.ShortenIt.common.service.CacheService.CACHE_L2S_PREFIX;
import static com.url.ShortenIt.common.service.CacheService.CACHE_S2L_PREFIX;

@RequiredArgsConstructor
@Service
public class RedirectServiceImpl implements RedirectService {

    private final UrlRepository urlRepository;
    private final CacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public UrlInfoResponse searchLongUrl(String shortUrl) {
        String longUrl = cacheService.get(CACHE_S2L_PREFIX + shortUrl);
        if (longUrl != null) {
            return new UrlInfoResponse(longUrl, shortUrl);
        }

        Optional<Url> url = urlRepository.findByShortUrl(shortUrl);
        Url found = url.orElseThrow(() -> new IllegalArgumentException("Short URL not found: " + shortUrl));
        if (found.isExpired()) {
            throw new IllegalStateException("This URL has expired: " + shortUrl);
        }
        Duration cacheTtl = cacheService.calculateCacheTtl(found.getExpiredAt());
        cacheService.put(CACHE_S2L_PREFIX + shortUrl, found.getLongUrl(), cacheTtl);
        cacheService.put(CACHE_L2S_PREFIX + found.getLongUrl(), shortUrl, cacheTtl);
        return new UrlInfoResponse(found.getLongUrl(), found.getShortUrl());
    }
}
