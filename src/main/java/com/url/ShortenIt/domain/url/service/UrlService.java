package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.dto.response.ShortUrlResponse;
import com.url.ShortenIt.domain.url.dto.response.UrlInfoResponse;


import java.time.Instant;

public interface UrlService {

    ShortUrlResponse saveShortUrl(String longUrl, Instant expiredAt);
    UrlInfoResponse searchLongUrl(String shortUrl);
    void deleteUrl(String shortUrl);
}
