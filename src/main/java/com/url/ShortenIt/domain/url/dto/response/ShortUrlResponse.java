package com.url.ShortenIt.domain.url.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "URL 단축 응답 DTO")
public record ShortUrlResponse(
        @NotBlank(message = "short_url은 필수입니다.")
        @Schema(description = "단축된 URL", example = "abc123")
        String shortUrl
) {
    public ShortUrlResponse {
        if (shortUrl == null || shortUrl.isBlank()) {
            throw new IllegalArgumentException("short_url은 필수입니다.");
        }
    }
}

