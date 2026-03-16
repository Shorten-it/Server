package com.url.ShortenIt.domain.url.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "URL 단축 요청 DTO")
public class LongUrlRequest {

    @Schema(description = "원본 URL", example = "https://www.example.com")
    @JsonProperty("long_url")
    private String longUrl;

    @Schema(description = "만료 시간 (ISO-8601)", example = "2026-12-31T23:59:59Z")
    @JsonProperty("expired_at")
    private Instant expiredAt;
}