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
public interface Urlrepository extends JpaRepository<Url, Long> {

    Optional<Url> findById(Long id);

    Optional<Url> findByLongUrl(String longUrl);
    Optional<Url> findByShortUrl(String shortUrl);

    List<Url> findAllByExpiredAtBeforeAndExpiredAtIsNotNull(Instant now);

    @Modifying
    @Query("DELETE FROM Url u WHERE u.expiredAt IS NOT NULL AND u.expiredAt < :now")
    int deleteAllExpiredBefore(@Param("now") Instant now);
}
