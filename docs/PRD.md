# Shortly — Product Requirements Document

> **작성일:** 2026-04-05 | **버전:** 1.0

---

## 1. Executive Summary

**Shortly**는 긴 URL을 짧은 코드로 변환하고, 해당 코드로 원본 URL에 리다이렉트하는 **MSA 기반 URL 단축 서비스**입니다.

**핵심 가치:** SnowFlake + Base62 기반의 충돌 없는 단축 코드 생성, Redis 양방향 캐싱을 통한 밀리초 단위 리다이렉트, 읽기/쓰기 분리 아키텍처로 독립적 스케일링을 지원합니다.

---

## 2. System Architecture

### 2.1 모듈 구조

| 모듈 | 포트 | 역할 | DB / 인프라 |
|------|------|------|------------|
| **API Gateway (Nginx)** | 80 | 라우팅, 로드밸런싱 (least_conn) | 없음 |
| **URL Service** | 8081 | URL 생성·삭제, 만료 URL 정리, 캐시 무효화 발행 | PostgreSQL + Redis |
| **Redirect Service** | 8082 | URL 리다이렉트·미리보기, 캐시 무효화 구독 | PostgreSQL + Redis |
| **common** | - | 공유 라이브러리 (Entity, DTO, Repository, Util) | - |

### 2.2 서비스 간 통신 흐름

```
[URL 생성/삭제 — 쓰기 경로]
클라이언트 → Nginx → URL Service → PostgreSQL 저장 + Redis 캐싱

[리다이렉트 — 읽기 경로]
클라이언트 → Nginx → Redirect Service → Redis 캐시 조회 (미스 시 PostgreSQL) → 301 Redirect

[캐시 무효화 — 유일한 서비스 간 통신]
URL Service → (URL 삭제 시) → Redis Pub/Sub → Redirect Service → 캐시 엔트리 삭제

[만료 URL 정리 — 배치]
URL Service → 매시간 정각 → expired_at < now() 인 URL 일괄 삭제
```

### 2.3 주요 설계 결정

| 결정 | 근거 |
|------|------|
| 읽기/쓰기 서비스 분리 | 각 워크로드에 최적화된 독립 스케일링 |
| PostgreSQL 공유 DB | 단일 진실 소스, 최종 일관성보다 단순한 구조 |
| Redis Pub/Sub 캐시 무효화 | 추가 인프라 없는 실시간 무효화, 서비스 간 디커플링 |
| SnowFlake + Base62 | 분산 환경 안전한 ID 생성 + URL-safe 컴팩트 인코딩 |
| TransactionSynchronization | DB 커밋 후 캐시 무효화 발행 보장 |
| Graceful Redis 실패 처리 | Redis 장애 시에도 서비스 정상 동작 |

---

## 3. 모듈 상세 명세

### 3.1 service-common (공유 라이브러리)

| 클래스 | 역할 |
|--------|------|
| `BaseEntity` | JPA 기본 엔티티 (createdAt, updatedAt, deletedAt) |
| `Url` | URL 도메인 엔티티 |
| `SnowFlake` | 분산 고유 ID 생성기 (64비트) |
| `BaseConversion` | Base62 인코딩/디코딩 |
| `LongUrlRequest` / `ShortUrlResponse` / `UrlInfoResponse` | 요청·응답 DTO |

### 3.2 URL Service (쓰기 최적화)

**역할:** URL 생성·삭제·만료 정리에 집중. 캐시 무효화 메시지 발행.

**도메인 모델:**

```
Url
├── urlId (Long, PK, IDENTITY)
├── longUrl (String, max 2048, NOT NULL)
├── shortUrl (String, max 50, UNIQUE, NOT NULL)
├── expiredAt (Instant, nullable)
├── createdAt / updatedAt / deletedAt (BaseEntity 상속)
```

**현재 구현된 기능:**

| 기능 | 설명 |
|------|------|
| URL 단축 | URL 정규화 → 캐시/DB 중복 확인 → SnowFlake ID 생성 → Base62 인코딩 → 저장 |
| URL 삭제 | DB 삭제 → 트랜잭션 커밋 후 Redis Pub/Sub로 캐시 무효화 발행 |
| 만료 URL 정리 | 매시간 정각 크론으로 `expired_at < now()` URL 일괄 삭제 |
| 양방향 캐싱 | L2S (Long→Short), S2L (Short→Long) 두 방향 캐시 저장 |

### 3.3 Redirect Service (읽기 최적화)

**역할:** URL 리다이렉트·미리보기에 집중. 캐시 무효화 메시지 구독.

**현재 구현된 기능:**

| 기능 | 설명 |
|------|------|
| 리다이렉트 | Redis 캐시 우선 조회 → 미스 시 DB → 만료 확인 → 301 Redirect |
| 미리보기 | 리다이렉트 없이 원본 URL을 JSON으로 반환 |
| 캐시 무효화 구독 | Redis Pub/Sub `cache:invalidation` 토픽 구독 → L2S, S2L 캐시 삭제 |

### 3.4 캐싱 전략

**Cache-Aside 패턴 (양방향):**

| 키 패턴 | 방향 | 용도 |
|---------|------|------|
| `L2S:{normalized_long_url}` | Long → Short | URL 생성 시 중복 방지 |
| `S2L:{short_url}` | Short → Long | 리다이렉트 시 빠른 조회 |

**TTL 정책:**

| 조건 | TTL |
|------|-----|
| 만료 시간 없음 | 24시간 |
| 만료 시간 있음 | min(남은 시간, 24시간) |
| 이미 만료됨 | 1초 |

**무효화:** Redis Pub/Sub (`cache:invalidation` 토픽), 메시지 형식 `shortUrl|longUrl`, DB 트랜잭션 커밋 후 발행

---

## 4. 기능 백로그 (MoSCoW 우선순위)

### 5.1 URL Service

| 우선순위 | 기능 | 수용 조건 | 상태 |
|---------|------|----------|------|
| Must Have | **URL 단축** | URL 입력 시 고유한 short code 생성, 중복 URL은 기존 코드 반환 | ✅ 완료 |
| Must Have | **URL 삭제** | short code로 삭제 시 DB 삭제 + 캐시 무효화 | ✅ 완료 |
| Must Have | **만료 URL 자동 정리** | 매시간 정각에 만료된 URL 일괄 삭제 | ✅ 완료 |

### 5.2 Redirect Service

| 우선순위 | 기능 | 수용 조건 | 상태 |
|---------|------|----------|------|
| Must Have | **301 리다이렉트** | short code 접근 시 원본 URL로 301 리다이렉트 | ✅ 완료 |
| Must Have | **미리보기** | 리다이렉트 없이 원본 URL JSON 조회 | ✅ 완료 |
| Must Have | **만료 URL 처리** | 만료된 URL 접근 시 410 Gone 반환 | ✅ 완료 |

### 5.3 인프라

| 우선순위 | 기능 | 수용 조건 | 상태 |
|---------|------|----------|------|
| Must Have | **Nginx 라우팅** | 메서드 기반 서비스 라우팅 | ✅ 완료 |
| Must Have | **Redis 캐싱 + Pub/Sub** | 양방향 캐싱, 실시간 무효화 | ✅ 완료 |
| Must Have | **Docker Compose** | 전체 서비스 원커맨드 기동 | ✅ 완료 |
| Should Have | **Rate Limiting** | IP당 요청 제한 | Backlog |

---

## 6. 로드맵

### Phase 1 — 핵심 기능 (현재) ✅

- 멀티모듈 MSA 아키텍처 (모놀리식 → MSA 전환 완료)
- URL 단축·리다이렉트·삭제
- Redis 양방향 캐싱 + Pub/Sub 캐시 무효화
- Nginx API Gateway 라우팅
- 만료 URL 자동 정리 스케줄러
- Docker 최적화 (.dockerignore, 레이어 캐싱, non-root 사용자)
- Swagger/OpenAPI 문서화

### Phase 2 — 분석·통계

- 클릭 수 추적 (short URL별 조회수)
- 리퍼러·지역별 분포 분석
- 시간대별 클릭 추이 그래프 API

### Phase 3 — 사용자 경험 향상

- 커스텀 별칭 지정
- 링크 메타데이터 미리보기 (OG 태그)
- QR 코드 자동 생성

### Phase 4 — 인프라 안정화 & 프로덕션

- Rate Limiting (API Gateway)
- Monitoring (Actuator + Prometheus + Grafana)
- DB 읽기 복제본
- Kubernetes 배포 + HPA
