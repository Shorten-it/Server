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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/url")
public class UrlController {

    private final UrlService urlService;
    @PostMapping("/shorten")
    public ApiResponse<ApiResponse.SuccessBody<String>> join(@RequestBody LongUrlRequest request) {
        String shortUrl =  urlService.saveShortUrl(request.longURL);
        return ApiResponseGenerator.success(shortUrl,HttpStatus.OK, MessageCode.SUCCESS);
    }


    @GetMapping("/{shortUrl}")
    public ResponseEntity<String> login(@PathVariable String shortUrl) {
        Url url = urlService.searchLongUrl(shortUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(url.getLongUrl()));
        return new ResponseEntity(headers, HttpStatus.MOVED_PERMANENTLY);

    }
}