import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * Scenario 3: Cache Stampede (최악 상황)
 * 인기 URL의 캐시를 수동 삭제한 직후 동시 요청을 집중시킨다.
 *
 * 사용법:
 *   1. 먼저 이 스크립트를 실행하여 URL 생성
 *   2. 로그에 출력된 redis-cli DEL 명령어를 복사
 *   3. 테스트 실행 중 별도 터미널에서 redis-cli DEL 실행
 *
 * 실행: k6 run k6/03-cache-stampede.js
 */
export const options = {
  maxRedirects: 0,
  stages: [
    { duration: '30s', target: 2000 },  // ramp-up
    { duration: '3m', target: 2000 },   // steady (이 구간에서 캐시 삭제)
    { duration: '10s', target: 0 },     // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(99)<200'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export function setup() {
  // 인기 URL 10개 생성
  const hotUrls = [];
  for (let i = 0; i < 10; i++) {
    const payload = JSON.stringify({
      long_url: `https://example.com/hot/${i}-${Date.now()}`,
    });
    const res = http.post(`${BASE_URL}/api/v1/url/shorten`, payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    if (res.status === 201) {
      hotUrls.push(res.json().short_url);
    }
  }

  // 캐시 워밍
  for (const shortUrl of hotUrls) {
    http.get(`${BASE_URL}/api/v1/url/${shortUrl}`);
  }

  // 캐시 삭제 명령어 출력
  console.log('=== 테스트 실행 중 아래 명령어로 캐시를 삭제하세요 ===');
  for (const shortUrl of hotUrls) {
    console.log(`redis-cli -a $REDIS_PASSWORD DEL "S2L:${shortUrl}"`);
  }
  console.log('=== 또는 한번에 삭제 ===');
  const keys = hotUrls.map((u) => `"S2L:${u}"`).join(' ');
  console.log(`redis-cli -a $REDIS_PASSWORD DEL ${keys}`);

  return { hotUrls };
}

export default function (data) {
  const { hotUrls } = data;
  if (!hotUrls || hotUrls.length === 0) return;

  // 인기 URL에 트래픽 집중 (80:20 분포 시뮬레이션)
  const idx = Math.random() < 0.8
    ? Math.floor(Math.random() * 3)           // 상위 3개에 80%
    : Math.floor(Math.random() * hotUrls.length); // 나머지 20%

  const res = http.get(`${BASE_URL}/api/v1/url/${hotUrls[idx]}`);

  check(res, {
    'status is 301': (r) => r.status === 301,
  });
}
