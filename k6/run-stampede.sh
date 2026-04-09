#!/bin/bash

# Cache Stampede 테스트 래퍼
# k6 실행 → ramp-up 대기 → Redis 캐시 삭제 → 결과 대기

set -e

REDIS_CONTAINER=${REDIS_CONTAINER:-shortly-redis-1}
REDIS_PASSWORD=${REDIS_PASSWORD:-your_redis_secret_password}

echo "[1/3] k6 Cache Stampede 테스트 시작 (백그라운드)"
k6 run k6/03-cache-stampede.js &
K6_PID=$!

echo "[2/3] ramp-up 대기 (40초)..."
sleep 40

echo "[3/3] Redis 캐시 삭제 (S2L:* 키)"
docker exec $REDIS_CONTAINER redis-cli -a $REDIS_PASSWORD --no-auth-warning KEYS "S2L:*" \
  | xargs -r docker exec -i $REDIS_CONTAINER redis-cli -a $REDIS_PASSWORD --no-auth-warning DEL

echo "캐시 삭제 완료. k6 종료 대기 중..."
wait $K6_PID
echo "테스트 완료."
