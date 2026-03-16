package com.url.ShortenIt.redirectservice.controller;

import com.url.ShortenIt.common.dto.response.UrlInfoResponse;
import com.url.ShortenIt.redirectservice.service.RedirectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/url")
@Tag(name = "URL 리디렉션", description = "URL 리디렉션 및 프리뷰 API")
public class RedirectController {

    private final RedirectService redirectService;

    @GetMapping("/{shortUrl}")
    @Operation(summary = "URL 리다이렉트", description = "단축된 URL을 리다이렉트합니다.")
    public ResponseEntity<Void> redirectToLongUrl(
            @Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        UrlInfoResponse urlInfo = redirectService.searchLongUrl(shortUrl);
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .location(URI.create(urlInfo.longUrl()))
                .build();
    }

    @GetMapping("/{shortUrl}/preview")
    @Operation(summary = "URL 미리보기", description = "리다이렉트 대신 원본 URL을 JSON으로 반환합니다.")
    public ResponseEntity<UrlInfoResponse> preview(
            @Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        UrlInfoResponse urlInfo = redirectService.searchLongUrl(shortUrl);
        return ResponseEntity.ok(urlInfo);
    }
}
