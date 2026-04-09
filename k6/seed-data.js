import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * 테스트 데이터 시딩: URL 1,000개 생성
 * 실행: k6 run k6/seed-data.js
 */
export const options = {
  vus: 10,
  iterations: 1000,
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export default function () {
  const payload = JSON.stringify({
    long_url: `https://example.com/page/${__VU}-${__ITER}-${Date.now()}`,
  });

  const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'created (201)': (r) => r.status === 201,
  });

  sleep(0.05);
}
