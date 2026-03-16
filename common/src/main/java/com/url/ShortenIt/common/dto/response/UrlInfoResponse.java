package com.url.ShortenIt.common.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "URL 정보 응답")
public record UrlInfoResponse(
        @Schema(description = "원본 URL")
        @JsonProperty("long_url")
        String longUrl,

        @Schema(description = "단축 URL")
        @JsonProperty("short_url")
        String shortUrl
) {}
