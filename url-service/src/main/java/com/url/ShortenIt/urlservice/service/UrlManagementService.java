package com.url.ShortenIt.urlservice.service;

import com.url.ShortenIt.common.dto.response.ShortUrlResponse;
import java.time.Instant;

public interface UrlManagementService {
    ShortUrlResponse saveShortUrl(String longUrl, Instant expiredAt);
    void deleteUrl(String shortUrl);
}
