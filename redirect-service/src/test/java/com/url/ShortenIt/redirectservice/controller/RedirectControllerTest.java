package com.url.ShortenIt.redirectservice.controller;

import com.url.ShortenIt.common.domain.Url;
import com.url.ShortenIt.common.repository.Urlrepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedirectControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private Urlrepository urlRepository;

    @AfterEach
    void tearDown() { urlRepository.deleteAll(); }

    @Test
    @DisplayName("URL 리다이렉트 테스트 - 성공")
    void testRedirect_Success() throws Exception {
        Url url = Url.create("https://www.google.com", "testShort123", null);
        urlRepository.save(url);

        mockMvc.perform(get("/api/v1/url/{shortUrl}", "testShort123"))
                .andDo(print())
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://www.google.com"));
    }

    @Test
    @DisplayName("URL 프리뷰 테스트 - 성공")
    void testPreview_Success() throws Exception {
        Url url = Url.create("https://www.github.com", "previewTest", null);
        urlRepository.save(url);

        mockMvc.perform(get("/api/v1/url/{shortUrl}/preview", "previewTest"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.long_url").value("https://www.github.com"))
                .andExpect(jsonPath("$.short_url").value("previewTest"));
    }

    @Test
    @DisplayName("존재하지 않는 URL 리다이렉트 - 실패")
    void testRedirect_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/url/{shortUrl}", "nonexistent"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
