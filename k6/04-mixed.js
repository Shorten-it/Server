import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Scenario 4: Mixed (현실 시뮬레이션)
 * Read:Write = 100:1 비율로 실제 트래픽을 재현한다.
 * 실행: k6 run k6/04-mixed.js
 */
export const options = {
  maxRedirects: 0,
  scenarios: {
    readers: {
      executor: 'constant-vus',
      vus: 100,
      duration: '3m',
      exec: 'read',
    },
    writers: {
      executor: 'constant-vus',
      vus: 1,
      duration: '3m',
      exec: 'write',
    },
  },
  thresholds: {
    'http_req_duration{scenario:readers}': ['p(99)<200'],
    'http_req_failed{scenario:readers}': ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export function setup() {
  const shortUrls = [];
  for (let i = 0; i < 1000; i++) {
    const payload = JSON.stringify({
      long_url: `https://example.com/mixed/${i}-${Date.now()}`,
    });
    const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    if (res.status === 201) {
      shortUrls.push(res.json().short_url);
    }
  }

  // 캐시 워밍
  for (const shortUrl of shortUrls) {
    http.get(`${BASE_URL}/api/v1/url/${shortUrl}`);
  }

  console.log(`Setup complete: ${shortUrls.length} URLs created & cache warmed`);
  return { shortUrls };
}

export function read(data) {
  const { shortUrls } = data;
  if (!shortUrls || shortUrls.length === 0) return;

  const shortUrl = shortUrls[Math.floor(Math.random() * shortUrls.length)];
  const res = http.get(`${BASE_URL}/api/v1/url/${shortUrl}`);

  check(res, {
    'read: status is 301': (r) => r.status === 301,
  });
}

export function write() {
  const payload = JSON.stringify({
    long_url: `https://example.com/mixed/new-${__VU}-${__ITER}-${Date.now()}`,
  });
  const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'write: created (201)': (r) => r.status === 201,
  });

  sleep(0.1);
}
