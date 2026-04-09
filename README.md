# Shortly Backend

> 긴 URL을 짧은 코드로 변환하고, 밀리초 단위로 리다이렉트하는 **읽기/쓰기 분리 MSA 기반 URL 단축 서비스**

---

## System Architecture


<img width="1007" height="489" alt="image" src="https://github.com/user-attachments/assets/92b2b42f-ce28-4c20-9ddd-b6f67a5e79da" />



---

## Tech Stack

![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis_7-DC382D?style=flat-square&logo=redis&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=flat-square&logo=nginx&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)

---

## Key Features

### 1. SnowFlake + Base62 기반 URL 단축
- SnowFlake 알고리즘으로 분산 환경 안전한 64비트 고유 ID 생성
- Base62 인코딩으로 URL-safe 컴팩트 단축 코드 변환
- URL 정규화 (https:// 자동 추가, URI 유효성 검증) + 중복 URL 감지

### 2. 양방향 Redis 캐싱 + Pub/Sub 실시간 무효화
- Cache-Aside 패턴: `L2S` (Long→Short), `S2L` (Short→Long) 두 방향 캐싱
- 만료 시간 기반 동적 TTL 계산 (기본 24시간, 만료 임박 시 자동 단축)
- URL 삭제 시 `TransactionSynchronization`으로 DB 커밋 후 Redis Pub/Sub 캐시 무효화 발행
- Redirect Service가 구독하여 실시간 캐시 엔트리 삭제 — 서비스 간 직접 호출 없이 디커플링

### 3. 읽기/쓰기 분리 MSA 아키텍처
- **URL Service** (쓰기 최적화): URL 생성·삭제·만료 정리에 집중
- **Redirect Service** (읽기 최적화): 리다이렉트·미리보기에 집중
- Nginx API Gateway에서 HTTP 메서드 기반 라우팅 (POST/DELETE → URL Service, GET → Redirect Service)
- 각 서비스 독립적 스케일링 가능

### 4. 만료 URL 자동 정리
- 매시간 정각 크론으로 `expired_at < now()` URL 일괄 삭제
- Graceful Redis 실패 처리 — Redis 장애 시에도 서비스 정상 동작

---

## Trouble Shooting

### 모놀리식 → MSA 전환: 읽기/쓰기 워크로드 분리
- **문제**: 단일 서비스에서 URL 생성(쓰기)과 리다이렉트(읽기)를 모두 처리하면, 읽기 트래픽 급증 시 쓰기 성능까지 저하되고 독립적 스케일링 불가
- **해결**: URL Service(쓰기)와 Redirect Service(읽기)로 분리, common 모듈로 도메인·DTO·리포지토리 공유, Nginx에서 메서드 기반 라우팅
- **결과**: 읽기/쓰기 서비스 독립 배포·스케일링 가능, 읽기 트래픽 급증 시 Redirect Service만 수평 확장

---

## 대규모 트래픽 가정

대규모 트래픽을 가정하고 시스템 개선을 진행합니다.

| 항목 | 수치 |
|------|------|
| **일일 리다이렉트** | 1억 건 |
| **읽기 피크 QPS** | ~3,500 |
| **Read:Write** | 100:1 |

### 규모 선정 이유

1. **실무 병목 재현**: 단일 DB의 한계치인 Peak 3,000 QPS를 의도적으로 유도하여 아키텍처의 한계점을 드러내기 위함
2. **개선 효과 정량화**: 문제가 실제로 발생하는 규모에서 테스트해야 개선 전후 차이를 데이터로 증명하기 위함
3. **선제적 용량 설계**: 미래 트래픽을 고려하여 사전에 대비하는 설계를 검증하기 위함

---

## 부하 테스트 시나리오

### 공통 조건

| 항목 | 값 |
|------|-----|
| 환경 | 로컬 Docker Compose |
| 테스트 데이터 | URL 1,000개 사전 생성 |
| 성공 기준 | p99 < 200ms, 에러율 < 1% |
| Docker 리소스 제한 | 없음 |
| 인스턴스 | 각 서비스 1대 |
| 시나리오당 시간 | 3분 (단계별은 단계당 3분) |

### 시나리오

| # | 시나리오 | 목적 | 방법 | 기대 |
|---|---------|------|------|------|
| 1 | **Baseline** (한계점 탐색) | 현재 아키텍처가 어디서 터지는지 확인 | VU를 단계별로 올림 (100 → 500 → 1000 → 2000) | 응답 시간, 에러율이 튀는 지점 = 병목 시작점 |
| 2 | **Cache Hit** (최적 상황) | 캐시가 다 맞을 때 최대 처리량 확인 | 1,000개 URL을 캐시 워밍 후 VU 2000으로 조회 | Redis만 치면 되니까 QPS 높게 나올 것 |
| 3 | **Cache Stampede** (최악 상황) | 인기 URL의 TTL 만료 시 DB 폭주 재현 | Redis 캐시를 수동 삭제한 직후 동시 요청 집중 | 커넥션 풀 고갈, 응답 시간 급등 |
| 4 | **Mixed** (현실 시뮬레이션) | Read:Write = 100:1 실제 트래픽 재현 | 읽기 VU 100개 + 쓰기 VU 1개 동시 수행 | 가장 현실적인 성능 수치 |
| 5 | **배치 삭제 중 읽기** (DB 경합) | 만료 URL 일괄 삭제가 읽기에 미치는 영향 측정 | 읽기 부하 건 상태에서 만료 삭제 스케줄러 트리거 | 읽기 응답 시간 변화 관찰 |

### 실행 순서

```
1. Baseline → 한계점 파악
2. Cache Hit + Cache Stampede → 캐시 계층 문제 확인
3. Mixed → 현실 부하에서 종합 확인
4. 배치 삭제 중 읽기 → DB 분리 필요성 확인
```

### 실행 방법

```bash
# 사전 준비: k6 설치
brew install k6

# 서비스 기동
docker compose up -d

# 1. Baseline
k6 run k6/01-baseline.js

# 2. Cache Hit
k6 run k6/02-cache-hit.js

# 3. Cache Stampede
# 스크립트 실행 후, 별도 터미널에서 로그에 출력된 redis-cli DEL 명령어 실행
k6 run k6/03-cache-stampede.js

# 4. Mixed
k6 run k6/04-mixed.js

# 5. 배치 삭제 중 읽기
# 스크립트 실행 후, steady 구간에서 스케줄러 수동 트리거
k6 run k6/05-batch-delete.js
```

> `BASE_URL` 변경 시: `k6 run -e BASE_URL=http://localhost:8080 k6/01-baseline.js`
