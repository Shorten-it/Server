package com.url.ShortenIt.domain.url.controller;



import com.url.ShortenIt.domain.support.ApiResponse;
import com.url.ShortenIt.domain.support.ApiResponseGenerator;
import com.url.ShortenIt.domain.support.MessageCode;
import com.url.ShortenIt.domain.url.domain.Url;
import com.url.ShortenIt.domain.url.dto.request.LongUrlRequest;
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
    public ApiResponse<?> createShortUrl(@RequestBody LongUrlRequest request) {
        String longUrl = request.getLongUrl();
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("long_url 값이 비어 있습니다.");
        }
        String shortUrl = urlService.saveShortUrl(longUrl);
        return ApiResponseGenerator.success(shortUrl, HttpStatus.OK, MessageCode.SUCCESS);
    }

    @GetMapping("/{shortUrl}")
    @Operation(summary = "URL 리다이렉트", description = "단축된 URL을 리다이렉트합니다.")
    public ResponseEntity<Void> redirectToLongUrl(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        Url url = urlService.searchLongUrl(shortUrl);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(url.getLongUrl()))
                .build();
    }

    @GetMapping("/{shortUrl}/preview")
    @Operation(summary = "URL 미리보기", description = "리다이렉트 대신 원본 URL을 JSON으로 반환합니다.")
    public ApiResponse<?> preview(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        Url url = urlService.searchLongUrl(shortUrl);
        return ApiResponseGenerator.success(url.getLongUrl(), HttpStatus.OK, MessageCode.SUCCESS);
    }
}