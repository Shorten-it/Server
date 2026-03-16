package com.url.ShortenIt.urlservice.controller;

import com.url.ShortenIt.common.dto.request.LongUrlRequest;
import com.url.ShortenIt.common.dto.response.ShortUrlResponse;
import com.url.ShortenIt.urlservice.service.UrlManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/url")
@Tag(name = "URL 관리", description = "URL 생성 및 삭제 API")
public class UrlManagementController {

    private final UrlManagementService urlManagementService;

    @PostMapping("/shorten")
    @Operation(summary = "URL 단축", description = "URL을 단축합니다.")
    public ResponseEntity<ShortUrlResponse> createShortUrl(@RequestBody LongUrlRequest request) {
        String longUrl = request.getLongUrl();
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("long_url 값이 비어 있습니다.");
        }
        ShortUrlResponse response = urlManagementService.saveShortUrl(longUrl, request.getExpiredAt());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortUrl}")
    @Operation(summary = "URL 삭제", description = "단축된 URL을 삭제합니다.")
    public ResponseEntity<Void> deleteUrl(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        urlManagementService.deleteUrl(shortUrl);
        return ResponseEntity.noContent().build();
    }
}
