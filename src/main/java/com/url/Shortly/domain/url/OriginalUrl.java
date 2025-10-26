package com.url.Shortly.domain.url;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OriginalUrl {

    @Column(name = "original_url", nullable = false, length = 2048)
    private String value;

    protected OriginalUrl() {
        // for JPA
    }

    private OriginalUrl(String value) {
        this.value = value;
    }

    public static OriginalUrl of(String value) {
        validate(value);
        return new OriginalUrl(value);
    }

    public String getValue() {
        return value;
    }

    private static void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OriginalUrl must not be blank");
        }
        // Basic URL format validation using java.net.URL
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("OriginalUrl is not a valid URL: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OriginalUrl that = (OriginalUrl) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}


