package com.url.ShortenIt.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@Entity
@Table(name = "url", indexes = {
    @Index(name = "idx_url_short_url", columnList = "shortUrl", unique = true),
    @Index(name = "idx_url_long_url", columnList = "longUrl", unique = true),
    @Index(name = "idx_url_expired_at", columnList = "expired_at")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Url extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "url_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 2048)
    private String longUrl;

    @Column(nullable = false, unique = true)
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
        return expiredAt != null && Instant.now().isAfter(expiredAt);
    }

    public void softDelete() {
        this.markDeleted();
    }
}
