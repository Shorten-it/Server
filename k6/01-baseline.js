import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Scenario 1: Baseline (한계점 탐색)
 * VU를 단계별로 올려 병목 시작점을 찾는다.
 * 실행: k6 run k6/01-baseline.js
 */
export const options = {
  maxRedirects: 0,
  stages: [
    { duration: '3m', target: 100 },
    { duration: '3m', target: 500 },
    { duration: '3m', target: 1000 },
    { duration: '3m', target: 2000 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export function setup() {
  // 1,000개 URL 생성 후 shortUrl 목록 수집
  const shortUrls = [];
  for (let i = 0; i < 1000; i++) {
    const payload = JSON.stringify({
      long_url: `https://example.com/baseline/${i}-${Date.now()}`,
    });
    const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    if (res.status === 201) {
      const body = res.json();
      shortUrls.push(body.short_url);
    }
  }
  console.log(`Setup complete: ${shortUrls.length} URLs created`);
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
