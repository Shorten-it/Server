package com.url.Shortly.domain.url;

import com.url.Shortly.domain.common.BaseEntity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "short_url", indexes = {
        @Index(name = "idx_short_code", columnList = "short_code", unique = true)
})
public class ShortUrl extends BaseEntity {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "short_code", nullable = false, unique = true, length = 16))
    })
    private ShortCode shortCode;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "value", column = @Column(name = "original_url", nullable = false, length = 2048))
    })
    private OriginalUrl originalUrl;

    @Column(name = "hit_count", nullable = false)
    private long hitCount = 0L;

    protected ShortUrl() {
        // for JPA
    }

    private ShortUrl(ShortCode shortCode, OriginalUrl originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
    }

    public static ShortUrl create(ShortCode shortCode, OriginalUrl originalUrl) {
        if (shortCode == null) {
            throw new IllegalArgumentException("shortCode must not be null");
        }
        if (originalUrl == null) {
            throw new IllegalArgumentException("originalUrl must not be null");
        }
        return new ShortUrl(shortCode, originalUrl);
    }

    public void increaseHitCount() {
        this.hitCount += 1;
    }

    public ShortCode getShortCode() {
        return shortCode;
    }

    public OriginalUrl getOriginalUrl() {
        return originalUrl;
    }

    public long getHitCount() {
        return hitCount;
    }
}


