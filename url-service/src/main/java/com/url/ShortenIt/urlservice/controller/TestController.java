package com.url.ShortenIt.urlservice.controller;

import com.url.ShortenIt.urlservice.scheduler.ExpiredUrlCleanupScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("docker")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test")
public class TestController {

    private final ExpiredUrlCleanupScheduler expiredUrlCleanupScheduler;

    @PostMapping("/trigger-cleanup")
    public ResponseEntity<Void> triggerCleanup() {
        expiredUrlCleanupScheduler.cleanupExpiredUrls();
        return ResponseEntity.ok().build();
    }
}
