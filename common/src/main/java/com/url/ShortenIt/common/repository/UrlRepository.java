package com.url.ShortenIt.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.url.ShortenIt.common.domain.Url;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByLongUrl(String longUrl);
    Optional<Url> findByShortUrl(String shortUrl);

    List<Url> findAllByExpiredAtBeforeAndExpiredAtIsNotNull(Instant now);

    @Modifying
    @Query("UPDATE Url u SET u.deletedAt = :now WHERE u.expiredAt IS NOT NULL AND u.expiredAt < :now AND u.deletedAt IS NULL")
    int softDeleteAllExpiredBefore(@Param("now") Instant now);
}
