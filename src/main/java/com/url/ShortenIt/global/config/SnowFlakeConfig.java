package com.url.ShortenIt.global.config;

import com.url.ShortenIt.domain.url.util.SnowFlake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowFlakeConfig {
    @Bean
    public SnowFlake snowFlake() {
        return new SnowFlake(1, 1);
    }
}
