package com.url.ShortenIt.domain.url.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "URL 정보 응답 DTO")
public record UrlInfoResponse(
        @NotBlank(message = "long_url은 필수입니다.")
        @Schema(description = "원본 URL", example = "https://www.example.com")
        String longUrl,
        
        @NotBlank(message = "short_url은 필수입니다.")
        @Schema(description = "단축된 URL", example = "abc123")
        String shortUrl
) {
    public UrlInfoResponse {
        if (longUrl == null || longUrl.isBlank()) {
            throw new IllegalArgumentException("long_url은 필수입니다.");
        }
        if (shortUrl == null || shortUrl.isBlank()) {
            throw new IllegalArgumentException("short_url은 필수입니다.");
        }
    }
}

