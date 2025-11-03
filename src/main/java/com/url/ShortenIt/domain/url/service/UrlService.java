package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.domain.Url;


public interface UrlService {

    String saveShortUrl(String longUrl);
    Url searchLongUrl(String shortUrl);

}
