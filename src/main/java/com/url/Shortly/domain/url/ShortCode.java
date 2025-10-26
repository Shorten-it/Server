package com.url.Shortly.domain.url;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ShortCode {

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 16;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String value;

    protected ShortCode() {
        // for JPA
    }

    private ShortCode(String value) {
        this.value = value;
    }

    public static ShortCode of(String value) {
        validate(value);
        return new ShortCode(value);
    }

    public String getValue() {
        return value;
    }

    private static void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ShortCode must not be blank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("ShortCode length must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
        }
        if (!value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("ShortCode must be URL-safe (alphanumeric, '-', '_')");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShortCode shortCode = (ShortCode) o;
        return Objects.equals(value, shortCode.value);
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


