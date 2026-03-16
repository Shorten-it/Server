package com.url.ShortenIt.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단축 URL 응답")
public record ShortUrlResponse(
        @Schema(description = "단축된 URL", example = "abc123")
        @JsonProperty("short_url")
        String shortUrl
) {}
