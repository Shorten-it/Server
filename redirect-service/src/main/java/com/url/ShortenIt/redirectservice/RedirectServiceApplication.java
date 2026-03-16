package com.url.ShortenIt.redirectservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.url.ShortenIt.common.domain")
@EnableJpaRepositories(basePackages = "com.url.ShortenIt.common.repository")
public class RedirectServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedirectServiceApplication.class, args);
    }
}
