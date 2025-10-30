package com.url.ShortenIt.domain.url.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.url.ShortenIt.domain.url.domain.Url;

@Repository
public interface Urlrepository extends JpaRepository<Url, Long> {

    Optional<Url> findById(Long id);

    Optional<Url> findByLongUrl(String longUrl);
    Optional<Url> findByShortUrl(String shortUrl); 
    
}
