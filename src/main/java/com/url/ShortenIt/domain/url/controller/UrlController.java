package com.url.ShortenIt.domain.url.controller;



import com.url.ShortenIt.domain.support.ApiResponse;
import com.url.ShortenIt.domain.support.ApiResponseGenerator;
import com.url.ShortenIt.domain.support.MessageCode;
import com.url.ShortenIt.domain.url.domain.Url;
import com.url.ShortenIt.domain.url.dto.request.LongUrlRequest;
import com.url.ShortenIt.domain.url.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    public ApiResponse<ApiResponse.SuccessBody<String>> join(@RequestBody LongUrlRequest request) {
        String shortUrl = urlService.saveShortUrl(request.longURL);
        return ApiResponseGenerator.success(shortUrl, HttpStatus.OK, MessageCode.SUCCESS);
    }

    @GetMapping("/{shortUrl}")
    @Operation(summary = "URL 리다이렉트", description = "단축된 URL을 리다이렉트합니다.")
    public ResponseEntity<Void> login(@Parameter(description = "단축된 URL", example = "abc123") @PathVariable String shortUrl) {
        Url url = urlService.searchLongUrl(shortUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getLongUrl()));
        return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
    }
}