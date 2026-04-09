import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Scenario 2: Cache Hit (최적 상황)
 * 캐시가 전부 맞을 때 최대 처리량을 확인한다.
 * 실행: k6 run k6/02-cache-hit.js
 */
export const options = {
  maxRedirects: 0,
  stages: [
    { duration: '30s', target: 2000 },  // ramp-up
    { duration: '3m', target: 2000 },   // steady
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
  for (let i = 0; i < 1000; i++) {
    const payload = JSON.stringify({
      long_url: `https://example.com/cache-hit/${i}-${Date.now()}`,
    });
    const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    if (res.status === 201) {
      shortUrls.push(res.json().short_url);
    }
  }

  // 캐시 워밍: 각 URL을 한 번씩 조회
  for (const shortUrl of shortUrls) {
    http.get(`${BASE_URL}/api/v1/url/${shortUrl}`);
  }

  console.log(`Setup complete: ${shortUrls.length} URLs created & cache warmed`);
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
