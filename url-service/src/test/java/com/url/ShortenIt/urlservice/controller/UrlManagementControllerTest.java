package com.url.ShortenIt.urlservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.ShortenIt.common.dto.request.LongUrlRequest;
import com.url.ShortenIt.common.dto.response.ShortUrlResponse;
import com.url.ShortenIt.common.repository.UrlRepository;
import com.url.ShortenIt.urlservice.service.UrlManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlManagementControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UrlManagementService urlManagementService;
    @Autowired private UrlRepository urlRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() { jdbcTemplate.execute("DELETE FROM url"); }

    @Test
    @DisplayName("URL 단축 생성 테스트 - 성공")
    void testShortenUrl_Success() throws Exception {
        String longUrl = "https://www.example.com";
        LongUrlRequest request = new LongUrlRequest();
        request.setLongUrl(longUrl);

        mockMvc.perform(post("/api/v1/url/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_url").exists())
                .andExpect(jsonPath("$.short_url").isString());

        assertThat(urlRepository.findByLongUrl(longUrl)).isPresent();
    }

    @Test
    @DisplayName("동일한 URL 재생성 시 기존 shortUrl 반환")
    void testShortenUrl_DuplicateUrl() throws Exception {
        String longUrl = "https://www.naver.com";
        ShortUrlResponse first = urlManagementService.saveShortUrl(longUrl, null);

        LongUrlRequest request = new LongUrlRequest();
        request.setLongUrl(longUrl);

        mockMvc.perform(post("/api/v1/url/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_url").value(first.shortUrl()));
    }

    @Test
    @DisplayName("URL 삭제 테스트 - 성공")
    void testDeleteUrl_Success() throws Exception {
        String longUrl = "https://www.google.com";
        ShortUrlResponse response = urlManagementService.saveShortUrl(longUrl, null);

        mockMvc.perform(delete("/api/v1/url/{shortUrl}", response.shortUrl()))
                .andDo(print())
                .andExpect(status().isNoContent());

        assertThat(urlRepository.findByShortUrl(response.shortUrl())).isEmpty();
    }
}
