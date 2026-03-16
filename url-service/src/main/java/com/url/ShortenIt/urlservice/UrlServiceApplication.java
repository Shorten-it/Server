package com.url.ShortenIt.urlservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EntityScan(basePackages = "com.url.ShortenIt.common.domain")
@EnableJpaRepositories(basePackages = "com.url.ShortenIt.common.repository")
@ComponentScan(basePackages = {"com.url.ShortenIt.urlservice", "com.url.ShortenIt.common.util"})
public class UrlServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrlServiceApplication.class, args);
    }
}
