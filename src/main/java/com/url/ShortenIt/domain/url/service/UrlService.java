package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.dto.response.ShortUrlResponse;
import com.url.ShortenIt.domain.url.dto.response.UrlInfoResponse;


public interface UrlService {

    ShortUrlResponse saveShortUrl(String longUrl);
    UrlInfoResponse searchLongUrl(String shortUrl);

}
