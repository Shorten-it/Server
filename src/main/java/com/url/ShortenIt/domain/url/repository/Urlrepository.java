package com.url.ShortenIt.domain.url.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;

import com.url.ShortenIt.domain.url.domain.Url;

@Repository
public interface Urlrepository extends JpaRepository<Url, Long> {

    Optional<Url> findById(Long id);

    Optional<Url> findByLongUrl(String longUrl);
    Optional<Url> findByShortUrl(String shortUrl);

    @Modifying
    @Query("DELETE FROM Url u WHERE u.expiredAt IS NOT NULL AND u.expiredAt < :now")
    int deleteAllExpiredBefore(@Param("now") Instant now);
}
