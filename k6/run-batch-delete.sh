#!/bin/bash

# 배치 삭제 중 읽기 테스트 래퍼
# k6 실행 → ramp-up 대기 → 만료 URL 삭제 스케줄러 트리거 → 결과 대기

set -e

URL_SERVICE=${URL_SERVICE:-http://localhost:8081}

echo "[1/3] k6 배치 삭제 테스트 시작 (백그라운드)"
k6 run k6/05-batch-delete.js &
K6_PID=$!

echo "[2/3] ramp-up 대기 (40초)..."
sleep 40

echo "[3/3] 만료 URL 삭제 스케줄러 수동 트리거"
curl -s -X POST "$URL_SERVICE/actuator/scheduledtasks" || echo "스케줄러 엔드포인트가 없으면 DB에서 직접 삭제:"
echo "  docker exec shortly-db-1 psql -U \$POSTGRES_USER -d \$POSTGRES_DB -c \"DELETE FROM url WHERE expired_at < NOW()\""

echo "k6 종료 대기 중..."
wait $K6_PID
echo "테스트 완료."
