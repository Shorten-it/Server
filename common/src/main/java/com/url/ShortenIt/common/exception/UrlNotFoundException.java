package com.url.ShortenIt.common.exception;

public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String shortUrl) {
        super("Short URL not found: " + shortUrl);
    }
}
