package com.url.ShortenIt.domain.url.service;

import com.url.ShortenIt.domain.url.domain.Url;
import com.url.ShortenIt.domain.url.repository.Urlrepository;
import com.url.ShortenIt.domain.url.util.BaseConversion;
import com.url.ShortenIt.domain.url.util.SnowFlake;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@AllArgsConstructor
@Service
public class UrlServiceIpml implements UrlService {

    private final Urlrepository Urlrepository;
    private final BaseConversion BaseConversion;

    @Override
    public String saveShortUrl(String longUrl) {
        Optional<Url> url = Urlrepository.findByLongUrl(longUrl);
        if (url.isPresent()) {
            return url.get().getShortUrl();
        }else{
            SnowFlake snowFlake = new SnowFlake(1,1);
            Long id = snowFlake.nextId();
            String str = BaseConversion.encode(id);
            Url urlEntity = Url.create(longUrl,str);
            Urlrepository.save(urlEntity);
            return urlEntity.getShortUrl();
        }

 

    }

    @Override
    public Url searchLongUrl(String shortUrl) {
        Optional<Url> url =  Urlrepository.findByShortUrl(shortUrl);
        return url.get();
    }
        
}