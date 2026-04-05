# Shortly - 이력서 & 면접 정리

## 프로젝트 한줄 소개
> 대규모 트래픽을 고려한 URL 단축 서비스를 MSA로 설계·구현하고, Redis 캐싱과 Pub/Sub을 활용한 다중 인스턴스 간 데이터 일관성을 보장하는 시스템

---

## 1. Redis Pub/Sub 기반 캐시 무효화

### 이력서 표현
> **문제**: URL 삭제 시 다중화된 서버 인스턴스 간 로컬 캐시 불일치로 리디렉션 오류 발생
> **해결**: URL 상태 변경 시 Redis Pub/Sub 토픽으로 메시지를 Publish하고, 모든 인스턴스가 Subscribe하여 즉시 캐시 무효화
> **성과**: 만료 URL 접근 시 리디렉션 오류율 0% 달성, 데이터 일관성 보장

### 면접 대비 Q&A

**Q: 왜 Redis Pub/Sub을 선택했나요?**
- 이미 캐시용으로 Redis를 사용 중이라 추가 인프라 비용 없음
- Fire-and-forget 방식으로 캐시 무효화에 적합 (메시지 유실 시 TTL로 자연 만료)
- Kafka 등 메시지 큐 대비 지연시간이 낮아 실시간 무효화에 유리

**Q: Pub/Sub 메시지 유실 시 어떻게 대응하나요?**
- Redis 캐시에 TTL(24시간)을 설정하여 메시지 유실 시에도 자연 만료
- Pub/Sub은 at-most-once 보장 → 캐시 무효화는 결과적 일관성(Eventual Consistency)으로 충분
- 데이터 정합성이 중요한 경우 Redis Streams나 Kafka 도입 고려

**Q: TransactionSynchronization을 왜 사용했나요?**
- DB 트랜잭션 커밋 전에 캐시를 무효화하면, 롤백 시 캐시만 삭제되는 불일치 발생
- `afterCommit()`에서 Publish하여 DB 커밋 성공 후에만 캐시 무효화 보장

**Q: Cache-Aside 패턴을 설명해주세요.**
- 읽기: 캐시 조회 → 미스 시 DB 조회 → 캐시에 저장
- 쓰기: DB 저장 → 캐시 갱신 또는 무효화
- 장점: 캐시 장애 시 DB로 fallback 가능, 구현이 단순
- 단점: 캐시 미스 시 지연시간 증가 (Cold Start)

---

## 2. Spring Scheduler 기반 만료 데이터 배치 삭제

### 이력서 표현
> **문제**: 만료된 URL 데이터가 DB에 누적되어 스토리지 낭비 및 쿼리 성능 저하
> **해결**: Spring Scheduler를 활용하여 만료 URL을 주기적으로 Bulk Delete하는 배치 작업 구현
> **성과**: URL 만료/삭제 라이프사이클 100% 구축, 불필요한 데이터 적재 방지

### 면접 대비 Q&A

**Q: 왜 Bulk Delete를 사용했나요?**
- 개별 DELETE 대비 DB I/O 횟수 감소 → 트랜잭션 오버헤드 최소화
- JPQL `DELETE` 쿼리로 영속성 컨텍스트를 거치지 않아 메모리 효율적
- 대량 데이터 삭제 시 인덱스 재구성 비용도 한 번만 발생

**Q: 스케줄러가 여러 인스턴스에서 동시 실행되면?**
- 현재: JPQL DELETE가 멱등성(Idempotent) → 중복 실행해도 문제 없음
- 개선 방안: ShedLock 라이브러리로 분산 락 적용, 단일 인스턴스만 실행하도록 제어
- 또는 @SchedulerLock으로 Redis/DB 기반 분산 락 설정

**Q: 매시간 실행인데, 더 세밀한 만료 처리가 필요하면?**
- Redis TTL을 활용하여 캐시 레벨에서 즉시 만료 처리 (현재 구현됨)
- DB 조회 시 `isExpired()` 체크로 스케줄러 실행 전에도 만료 URL 접근 차단
- 실시간 필요 시: Redis Keyspace Notifications로 만료 이벤트 수신

**Q: `expiredAt` 필드 인덱스는?**
- WHERE 조건에 자주 사용되므로 복합 인덱스 `(expired_at, url_id)` 추가 권장
- NULL이 많은 컬럼이므로 Partial Index 고려 (`WHERE expired_at IS NOT NULL`)

---

## 3. Nginx 로드 밸런싱 & API Gateway

### 이력서 표현
> **문제**: 단일 서버 운영으로 트래픽 급증 시 SPOF(Single Point of Failure) 발생
> **해결**: Nginx를 API Gateway로 도입, Least Connection 방식 로드 밸런싱 및 경로 기반 서비스 라우팅 구현
> **성과**: 서버 부하 분산으로 트래픽 처리 능력 향상 및 SPOF 완화

### 면접 대비 Q&A

**Q: Least Connection 방식을 선택한 이유는?**
- Round Robin: 모든 서버에 균등 분배 → 처리 시간이 다른 요청에 불리
- Least Connection: 현재 연결 수가 가장 적은 서버에 분배 → URL 리디렉션처럼 응답시간 편차가 큰 서비스에 적합
- IP Hash: 세션 유지 필요 시 → Stateless 서비스이므로 불필요

**Q: MSA에서 Nginx의 역할은?**
- **API Gateway**: 클라이언트 단일 진입점, 내부 서비스 라우팅
- **로드 밸런서**: 동일 서비스 다중 인스턴스 간 부하 분산
- **리버스 프록시**: 내부 서비스 포트 은닉, 보안 헤더 추가
- Spring Cloud Gateway 대비: 더 가볍고 성능 우수, L7 수준 라우팅 가능

**Q: Nginx가 SPOF가 되지 않나요?**
- 맞음. Nginx 자체의 HA를 위해 Keepalived + VIP 구성 필요
- 클라우드 환경에서는 AWS ALB/NLB 등 관리형 로드밸런서 사용
- Docker Swarm/K8s에서는 내장 Service Discovery와 로드밸런싱 활용

---

## 4. MSA 전환

### 이력서 표현
> **설계**: 모놀리식 서비스를 쓰기 최적화(url-service)와 읽기 최적화(redirect-service)로 분리하여 독립 배포·확장 가능한 MSA 구현
> **성과**: 서비스별 독립 스케일링 가능, Redis Pub/Sub 기반 비동기 서비스 간 통신으로 느슨한 결합 달성

### 면접 대비 Q&A

**Q: 왜 MSA로 전환했나요?**
- URL 리디렉션(읽기)은 트래픽의 90% 이상, URL 생성(쓰기)은 10% 미만
- 읽기/쓰기 서비스를 분리하여 독립 스케일링 → redirect-service만 3~5배 확장 가능
- 서비스별 독립 배포로 장애 격리 및 배포 주기 단축

**Q: 서비스를 어떤 기준으로 분리했나요?**
- **CQRS 패턴** 기반: Command(쓰기) vs Query(읽기) 분리
- url-service: 쓰기 책임 (생성, 삭제, 스케줄러) → 데이터 무결성 중심
- redirect-service: 읽기 책임 (리디렉션, 프리뷰) → 응답 속도 중심
- common 모듈: 공유 엔티티/DTO로 코드 중복 제거

**Q: 공유 DB를 사용하면 MSA의 장점이 줄어들지 않나요?**
- 맞음. 현재는 공유 DB(PostgreSQL)로 전환 비용 최소화
- 향후 개선: url-service는 Write DB, redirect-service는 Read Replica 사용
- 또는 redirect-service는 Redis만 의존하고, Cache Miss 시 url-service API 호출로 완전 분리

**Q: 서비스 간 통신은 어떻게 하나요?**
- **동기 통신**: 현재 없음 (공유 DB로 간접 통신)
- **비동기 통신**: Redis Pub/Sub으로 캐시 무효화 이벤트 전파
- 향후: gRPC로 서비스 간 직접 통신, 또는 이벤트 소싱 도입 가능

**Q: Docker Compose의 한계와 대안은?**
- 한계: 단일 호스트, 오토스케일링 불가, 셀프 힐링 없음
- 대안: Kubernetes (자동 스케일링, 서비스 디스커버리, 롤링 업데이트)
- 현 단계에서는 Docker Compose로 충분, K8s는 트래픽 증가 시 전환

---

## 5. 기술적 깊이 (공통)

### SnowFlake ID 생성
- Twitter에서 개발한 분산 ID 생성 알고리즘
- 64비트: 타임스탬프(41) + 데이터센터(5) + 머신(5) + 시퀀스(12)
- Auto Increment 대비 장점: 분산 환경에서 충돌 없이 ID 생성, ID로 생성 시간 추정 가능
- Base62 인코딩으로 짧은 URL 문자열 생성

### Redis 캐시 전략
- **L2S (Long to Short)**: 동일 URL 중복 생성 방지
- **S2L (Short to Long)**: 리디렉션 시 DB 조회 최소화
- **TTL**: 기본 24시간, 만료 시간이 설정된 URL은 남은 시간 기반 동적 TTL
- **캐시 무효화**: Pub/Sub + TTL 이중 전략

### 테스트 전략
- `@SpringBootTest` + `@AutoConfigureMockMvc`: 통합 테스트
- Redis 자동설정 제외: 테스트 환경에서 외부 의존성 격리
- `@AfterEach` cleanup: 테스트 간 데이터 격리
- H2 인메모리 DB: PostgreSQL 호환 모드로 로컬 테스트
