package com.url.ShortenIt.domain.url.domain;

import com.url.ShortenIt.domain.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.time.Instant;

@Entity
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Url extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "url_id")
    private Long id;

    @Column(nullable = false)
    private String longUrl;

    @Column(nullable = false)
    private String shortUrl;

    @Column(name = "expired_at")
    private Instant expiredAt;

    public static Url create(String longUrl, String shortUrl) {
        return Url.builder()
            .longUrl(longUrl)
            .shortUrl(shortUrl)
            .build();
    }

    public static Url create(String longUrl, String shortUrl, Instant expiredAt) {
        return Url.builder()
            .longUrl(longUrl)
            .shortUrl(shortUrl)
            .expiredAt(expiredAt)
            .build();
    }

    public boolean isExpired() {
        return isExpired(Instant.now());
    }

    public boolean isExpired(Instant now) {
        return expiredAt != null && now.isAfter(expiredAt);
    }
}
