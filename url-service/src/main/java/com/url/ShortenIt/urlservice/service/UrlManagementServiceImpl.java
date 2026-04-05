package com.url.ShortenIt.urlservice.service;

import com.url.ShortenIt.common.domain.Url;
import com.url.ShortenIt.common.dto.response.ShortUrlResponse;
import com.url.ShortenIt.common.exception.UrlNotFoundException;
import com.url.ShortenIt.common.repository.UrlRepository;
import com.url.ShortenIt.common.service.CacheService;
import com.url.ShortenIt.common.util.BaseConversion;
import com.url.ShortenIt.common.util.SnowFlake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.url.ShortenIt.common.service.CacheService.CACHE_L2S_PREFIX;
import static com.url.ShortenIt.common.service.CacheService.CACHE_S2L_PREFIX;

@Slf4j
@RequiredArgsConstructor
@Service
public class UrlManagementServiceImpl implements UrlManagementService {

    private final UrlRepository urlRepository;
    private final BaseConversion baseConversion;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;
    private final SnowFlake snowFlake;
    private final CacheService cacheService;

    @Override
    @Transactional
    public ShortUrlResponse saveShortUrl(String longUrl, Instant expiredAt) {
        String normalized = normalizeLongUrl(longUrl);

        String cachedShort = cacheService.get(CACHE_L2S_PREFIX + normalized);
        if (cachedShort != null) {
            return new ShortUrlResponse(cachedShort);
        }

        Optional<Url> url = urlRepository.findByLongUrl(normalized);
        if (url.isPresent()) {
            String shortUrl = url.get().getShortUrl();
            cacheService.put(CACHE_L2S_PREFIX + normalized, shortUrl);
            cacheService.put(CACHE_S2L_PREFIX + shortUrl, normalized);
            log.info("Existing short URL returned: {} -> {}", shortUrl, normalized);
            return new ShortUrlResponse(shortUrl);
        } else {
            Long id = snowFlake.nextId();
            String str = baseConversion.encode(id);
            Url urlEntity = Url.create(normalized, str, expiredAt);
            urlRepository.save(urlEntity);
            Duration cacheTtl = cacheService.calculateCacheTtl(expiredAt);
            cacheService.put(CACHE_L2S_PREFIX + normalized, str, cacheTtl);
            cacheService.put(CACHE_S2L_PREFIX + str, normalized, cacheTtl);
            log.info("New short URL created: {} -> {}", str, normalized);
            return new ShortUrlResponse(urlEntity.getShortUrl());
        }
    }

    @Override
    @Transactional
    public void deleteUrl(String shortUrl) {
        Url url = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException(shortUrl));
        String longUrl = url.getLongUrl();
        url.softDelete();
        log.info("URL deleted: {} -> {}", shortUrl, longUrl);
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
}
