import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 20,
  duration: '30s',
  // 리다이렉트 자동 추적 방지: 원본 301/302 상태를 그대로 보게 함
  maxRedirects: 0,
};

const BASE_URL = 'http://localhost:8080/api/v1/url';

export function setup() {
  // 캐시에 넣을 고정 URL을 하나 생성
  const payload = JSON.stringify({ longURL: 'https://example.org/perf-test' });
  const headers = { 'Content-Type': 'application/json' };
  const res = http.post(`${BASE_URL}/shorten`, payload, { headers });

  // 응답 바디는 { data: string, message: string, code: string }
  const body = res.json();
  const shortCode = body && body.data ? body.data : null;

  if (!shortCode) {
    throw new Error('shortCode 생성 실패: POST /shorten 응답에 data가 없습니다.');
  }
  console.log(`shortCode(from setup) = ${shortCode}`);

  return { shortCode };
}

export default function (data) {
  const { shortCode } = data;
  // 리다이렉트를 따르지 않고 원 응답을 검증
  const res = http.get(`${BASE_URL}/${shortCode}`);

  // 컨트롤러는 301(MOVED_PERMANENTLY)로 리다이렉트
  check(res, {
    'status is 301/302': (r) => r.status === 301 || r.status === 302,
  });

  // 상태 0(네트워크 실패)나 4xx/5xx를 초기에 일부만 로깅
  if ((res.status === 0 || res.status >= 400) && __ITER < 10) {
    console.error(`iter=${__ITER} status=${res.status} error=${res.error || ''}`);
  }
}

export function handleSummary(data) {
  const statusCounts = data.metrics['http_req_duration'].values ? data.metrics : data.metrics;
  // 간단 요약만 출력 (k6 기본 요약 외에 상태코드 분포 확인을 권장)
  return {
    stdout: `\nHint: 3xx가 실패로 집계되면 네트워크 실패(상태 0) 가능성이 큽니다. BASE_URL, 포트, 방화벽 확인하세요.\n`,
  };
}


