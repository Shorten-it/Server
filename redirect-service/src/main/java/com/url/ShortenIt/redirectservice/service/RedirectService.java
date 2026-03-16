package com.url.ShortenIt.redirectservice.service;

import com.url.ShortenIt.common.dto.response.UrlInfoResponse;

public interface RedirectService {
    UrlInfoResponse searchLongUrl(String shortUrl);
}
