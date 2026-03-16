package com.url.ShortenIt.domain.url.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.ShortenIt.domain.url.dto.request.LongUrlRequest;
import com.url.ShortenIt.domain.url.dto.response.ShortUrlResponse;
import com.url.ShortenIt.domain.url.repository.Urlrepository;
import com.url.ShortenIt.domain.url.service.UrlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlService urlService;

    @Autowired
    private Urlrepository urlRepository;

    @AfterEach
    void tearDown() {
        urlRepository.deleteAll();
    }

    @Test
    @DisplayName("URL 단축 생성 테스트 - 성공")
    void testShortenUrl_Success() throws Exception {
        // given
        String longUrl = "https://www.example.com";
        LongUrlRequest request = new LongUrlRequest();
        request.setLongUrl(longUrl);

        // when & then
        mockMvc.perform(post("/api/v1/url/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_url").exists())
                .andExpect(jsonPath("$.short_url").isString());

        // 저장된 URL 확인
        assertThat(urlRepository.findByLongUrl(longUrl)).isPresent();
    }

    @Test
    @DisplayName("URL 리다이렉트 조회 테스트 - 성공")
    void testGetLongUrl_Success() throws Exception {
        // given
        String longUrl = "https://www.google.com";
        ShortUrlResponse response = urlService.saveShortUrl(longUrl, null);
        String shortUrl = response.shortUrl();

        // when & then
        mockMvc.perform(get("/api/v1/url/{shortUrl}", shortUrl))
                .andDo(print())
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", longUrl));
    }

    @Test
    @DisplayName("동일한 URL 재생성 시 기존 shortUrl 반환 테스트")
    void testShortenUrl_DuplicateUrl_ReturnsExistingShortUrl() throws Exception {
        // given
        String longUrl = "https://www.naver.com";
        LongUrlRequest request = new LongUrlRequest();
        request.setLongUrl(longUrl);

        // 첫 번째 생성
        ShortUrlResponse firstResponse = urlService.saveShortUrl(longUrl, null);
        String firstShortUrl = firstResponse.shortUrl();

        // 두 번째 생성 요청
        mockMvc.perform(post("/api/v1/url/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_url").value(firstShortUrl));

        // 동일한 shortUrl이 반환되었는지 확인
        assertThat(urlRepository.findByLongUrl(longUrl)).isPresent();
        assertThat(urlRepository.findByLongUrl(longUrl).get().getShortUrl()).isEqualTo(firstShortUrl);
    }

    @Test
    @DisplayName("URL 생성 후 조회 전체 플로우 테스트")
    void testCreateAndRetrieveUrl_FullFlow() throws Exception {
        // given
        String longUrl = "https://www.github.com";
        LongUrlRequest request = new LongUrlRequest();
        request.setLongUrl(longUrl);

        // when - URL 생성
        mockMvc.perform(post("/api/v1/url/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.short_url").exists());

        // shortUrl 추출
        String shortUrl = urlRepository.findByLongUrl(longUrl)
                .orElseThrow()
                .getShortUrl();

        // then - 생성된 shortUrl로 조회
        mockMvc.perform(get("/api/v1/url/{shortUrl}", shortUrl))
                .andDo(print())
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", longUrl));

        // 데이터베이스에서도 확인
        assertThat(urlRepository.findByShortUrl(shortUrl)).isPresent();
        assertThat(urlRepository.findByShortUrl(shortUrl).get().getLongUrl()).isEqualTo(longUrl);
    }
}
