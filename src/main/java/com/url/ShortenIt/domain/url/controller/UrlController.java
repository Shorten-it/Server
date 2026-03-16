package com.url.ShortenIt.domain.url.controller;



import com.url.ShortenIt.domain.url.dto.request.LongUrlRequest;
import com.url.ShortenIt.domain.url.dto.response.ShortUrlResponse;
import com.url.ShortenIt.domain.url.dto.response.UrlInfoResponse;
import com.url.ShortenIt.domain.url.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/url")
@Tag(name = "URL 단축", description = "URL 단축 및 리다이렉트 API")
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    @Operation(summary = "URL 단축", description = "URL을 단축합니다.")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@RequestBody LongUrlRequest request) {
        String longUrl = request.getLongUrl();
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("long_url 값이 비어 있습니다.");
        }
        ShortUrlResponse response = urlService.saveShortUrl(longUrl, request.getExpiredAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortUrl}")
    @Operation(summary = "URL 리다이렉트", description = "단축된 URL을 리다이렉트합니다.")
    public ResponseEntity<Void> redirectToLongUrl(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        UrlInfoResponse urlInfo = urlService.searchLongUrl(shortUrl);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(urlInfo.longUrl()))
                .build();
    }

    @GetMapping("/{shortUrl}/preview")
    @Operation(summary = "URL 미리보기", description = "리다이렉트 대신 원본 URL을 JSON으로 반환합니다.")
    public ResponseEntity<UrlInfoResponse> preview(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        UrlInfoResponse urlInfo = urlService.searchLongUrl(shortUrl);
        return ResponseEntity.ok(urlInfo);
    }

    @DeleteMapping("/{shortUrl}")
    @Operation(summary = "URL 삭제", description = "단축된 URL을 삭제합니다.")
    public ResponseEntity<Void> deleteUrl(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        urlService.deleteUrl(shortUrl);
        return ResponseEntity.noContent().build();
    }
}