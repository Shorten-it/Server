import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Scenario 5: 배치 삭제 중 읽기 (DB 경합)
 * 읽기 부하를 건 상태에서 만료 URL 삭제 스케줄러를 트리거한다.
 *
 * 사용법:
 *   1. 스크립트 실행 (읽기 부하 시작)
 *   2. steady 구간에서 별도 터미널로 스케줄러 수동 트리거:
 *      curl -X POST http://localhost:8081/actuator/scheduledtasks (또는 직접 DB 삭제 쿼리)
 *
 * 실행: k6 run k6/05-batch-delete.js
 */
export const options = {
  maxRedirects: 0,
  stages: [
    { duration: '30s', target: 1000 },  // ramp-up
    { duration: '3m', target: 1000 },   // steady (이 구간에서 배치 삭제 트리거)
    { duration: '10s', target: 0 },     // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(99)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export function setup() {
  const shortUrls = [];

  // 일반 URL 800개
  for (let i = 0; i < 800; i++) {
    const payload = JSON.stringify({
      long_url: `https://example.com/batch/${i}-${Date.now()}`,
    });
    const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    if (res.status === 201) {
      shortUrls.push(res.json().short_url);
    }
  }

  // 만료 URL 200개 (expired_at을 과거로 설정)
  for (let i = 0; i < 200; i++) {
    const payload = JSON.stringify({
      long_url: `https://example.com/expired/${i}-${Date.now()}`,
      expired_at: '2020-01-01T00:00:00Z',
    });
    http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
  }

  // 캐시 워밍 (일반 URL만)
  for (const shortUrl of shortUrls) {
    http.get(`${BASE_URL}/api/v1/url/${shortUrl}`);
  }

  console.log(`Setup complete: ${shortUrls.length} normal + 200 expired URLs created`);
  console.log('=== steady 구간에서 스케줄러를 수동 트리거하세요 ===');
  return { shortUrls };
}

export default function (data) {
  const { shortUrls } = data;
  if (!shortUrls || shortUrls.length === 0) return;

  const shortUrl = shortUrls[Math.floor(Math.random() * shortUrls.length)];
  const res = http.get(`${BASE_URL}/api/v1/url/${shortUrl}`);

  check(res, {
    'status is 301': (r) => r.status === 301,
  });
}
