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

### (도입 예정) Redirect Service 다중화 시 레이어드 캐시 + Pub/Sub 로컬 캐시 무효화
- **문제**: Redirect Service를 다중화하면, 한 인스턴스의 Local 캐시를 삭제해도 다른 인스턴스에 stale 데이터가 남아 삭제된 URL로 계속 리다이렉트
- **해결 (예정)**: L1(Caffeine) → L2(Redis) 레이어드 캐시 구성. Redis Pub/Sub로 무효화 메시지를 발행하면 모든 Redirect Service 인스턴스가 구독하여 자신의 L1 Local 캐시와 L2 Redis 캐시를 동시에 삭제
- **기대 결과 (예상)**: L1 로컬 캐시로 Redis 네트워크 홉 없는 초고속 응답 + Pub/Sub를 통한 전 인스턴스 L1·L2 동시 무효화로 캐시 일관성 보장

### (도입 예정) 캐시 스탬피드(Cache Stampede) 방어
- **문제**: 인기 short URL의 Redis TTL이 만료되는 순간, 동시 요청들이 일제히 캐시 미스 → DB 조회를 발생시켜 DB에 순간 부하 집중
- **해결**: Redis 분산 락(Redisson Lock) 적용 — 캐시 미스 시 하나의 요청만 락을 획득하여 DB 조회 후 캐시를 갱신하고, 나머지 요청은 갱신된 캐시를 사용
- **결과**: TTL 만료 시점의 DB 부하 스파이크 제거, 동시 요청 중 단 1건만 DB에 접근하여 경합 원천 차단

### (도입 예정) 읽기/쓰기 DB 분리
- **문제**: URL Service(쓰기)와 Redirect Service(읽기)가 단일 PostgreSQL을 공유하여, 만료 URL 일괄 삭제 시 리다이렉트 읽기 쿼리와 경합 발생
- **해결 (예정)**: Write DB + Read DB로 분리하고 주기적 동기화. 쓰기 시 캐시에 데이터를 먼저 적재하여 동기화 완료 전에도 읽기 서비스가 캐시에서 즉시 응답 가능하도록 설계
- **기대 결과 (예상)**: 쓰기 부하(삭제·생성)와 읽기 부하(리다이렉트) 완전 분리, 동기화 지연 구간에서도 캐시 선적재로 사용자 체감 지연 없음
