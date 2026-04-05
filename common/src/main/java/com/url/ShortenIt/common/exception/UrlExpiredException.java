package com.url.ShortenIt.common.exception;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortUrl) {
        super("This URL has expired: " + shortUrl);
    }
}
