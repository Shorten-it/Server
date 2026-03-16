# Shortly MSA 아키텍처

## 개요
모놀리식 URL 단축 서비스를 마이크로서비스 아키텍처(MSA)로 전환하여 서비스별 독립 배포 및 확장이 가능한 구조로 변경.

## 아키텍처 다이어그램

```
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │ :80
                    ┌──────▼──────┐
                    │    Nginx    │
                    │ (API Gateway)│
                    └──┬───────┬──┘
          POST/DELETE  │       │  GET (redirect/preview)
                ┌──────▼──┐ ┌──▼────────┐
                │url-service│ │redirect-  │
                │  :8081   │ │ service   │
                │          │ │  :8082    │
                └────┬─────┘ └────┬──────┘
                     │   Pub/Sub  │
                ┌────▼────────────▼──┐
                │       Redis        │
                │    (Cache + Pub/Sub)│
                │       :6379        │
                └────────────────────┘
                ┌────────────────────┐
                │    PostgreSQL      │
                │    (Shared DB)     │
                │       :5432        │
                └────────────────────┘
```

## 서비스 구성

### 1. url-service (포트: 8081)
- **역할**: URL 생성, 삭제, 만료 데이터 배치 삭제
- **엔드포인트**:
  - `POST /api/v1/url/shorten` — URL 단축 생성
  - `DELETE /api/v1/url/{shortUrl}` — URL 삭제
- **주요 기능**:
  - SnowFlake + Base62 기반 단축 URL 생성
  - Redis Cache-Aside 패턴 적용
  - 삭제 시 Redis Pub/Sub으로 캐시 무효화 메시지 발행
  - Spring Scheduler로 매시간 만료 URL Bulk Delete

### 2. redirect-service (포트: 8082)
- **역할**: URL 리디렉션, 프리뷰 (읽기 최적화)
- **엔드포인트**:
  - `GET /api/v1/url/{shortUrl}` — 301 리디렉션
  - `GET /api/v1/url/{shortUrl}/preview` — 원본 URL JSON 반환
- **주요 기능**:
  - Redis 캐시 우선 조회 (Cache-Aside)
  - Redis Pub/Sub 구독하여 캐시 무효화 수신
  - 만료된 URL 접근 시 410 Gone 응답

### 3. common 모듈
- **역할**: 서비스 간 공유 코드 (라이브러리 JAR)
- **포함 내용**: Url 엔티티, BaseEntity, DTO, Repository, 유틸 (BaseConversion, SnowFlake)

### 4. Nginx (API Gateway, 포트: 80)
- **역할**: 요청 라우팅 및 로드 밸런싱
- **라우팅 규칙**:
  - `POST /api/v1/url/shorten` → url-service
  - `DELETE /api/v1/url/{shortUrl}` → url-service
  - `GET /api/v1/url/{shortUrl}` → redirect-service
  - `GET /api/v1/url/{shortUrl}/preview` → redirect-service

## 기술 스택
| 구성 요소 | 기술 |
|-----------|------|
| 언어 | Java 17 |
| 프레임워크 | Spring Boot 3.5.7 |
| 빌드 | Gradle (멀티 모듈) |
| DB | PostgreSQL 16 |
| 캐시 | Redis 7 |
| API Gateway | Nginx |
| 컨테이너 | Docker + Docker Compose |

## 실행 방법

### 로컬 개발 (H2)
```bash
# url-service 실행
./gradlew :url-service:bootRun

# redirect-service 실행
./gradlew :redirect-service:bootRun
```

### Docker Compose (PostgreSQL + Redis)
```bash
# .env 파일 생성
cat > .env << EOF
POSTGRES_DB=shortly
POSTGRES_USER=shortly
POSTGRES_PASSWORD=shortly123
REDIS_PASSWORD=redis123
EOF

# 전체 서비스 실행
docker compose up --build

# 개별 서비스 로그 확인
docker compose logs -f url-service
docker compose logs -f redirect-service
```

### 테스트
```bash
# 전체 테스트
./gradlew clean test

# 모듈별 테스트
./gradlew :url-service:test
./gradlew :redirect-service:test
```

## 환경변수
| 변수 | 설명 | 기본값 |
|------|------|--------|
| POSTGRES_DB | PostgreSQL DB 이름 | - |
| POSTGRES_USER | PostgreSQL 사용자 | - |
| POSTGRES_PASSWORD | PostgreSQL 비밀번호 | - |
| REDIS_PASSWORD | Redis 비밀번호 | - |
| SPRING_PROFILES_ACTIVE | 활성 프로필 | default (H2) |

## 서비스 간 통신
- **동기 통신**: 없음 (각 서비스가 공유 DB에 직접 접근)
- **비동기 통신**: Redis Pub/Sub (`cache:invalidation` 토픽)
  - url-service → Publisher (URL 삭제 시 캐시 무효화 메시지 발행)
  - redirect-service → Subscriber (메시지 수신 후 로컬 Redis 캐시 삭제)
