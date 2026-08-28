#!/bin/bash
# TRUNCATE all domain tables and reload from k6/seed/snapshot.sql.gz, then flush Redis.
# Used between k6 phases to give every comparable test run an identical starting dataset.
set -e

cd "$(dirname "$0")/.."

docker compose exec -T postgres psql -U otboo_user -d otboo -v ON_ERROR_STOP=1 -c "
TRUNCATE TABLE
  batch_job_execution_context, batch_job_execution_params, batch_step_execution_context,
  batch_step_execution, batch_job_execution, batch_job_instance,
  notifications, notification_outboxes,
  comments, direct_messages, feed_likes, feeds,
  clothes_attributes, clothes_attribute_def_values, clothes_attribute_defs,
  follows, social_accounts, profiles, clothes, locations,
  weather_d1_baselines, weathers, weather_grids,
  users
RESTART IDENTITY CASCADE;
"

gunzip -c k6/seed/snapshot.sql.gz | docker compose exec -T postgres psql -U otboo_user -d otboo -v ON_ERROR_STOP=1 -q

docker compose exec -T redis redis-cli FLUSHALL > /dev/null

# 인덱스/매핑/alias는 건드리지 않고 문서만 전부 비운다(alias 마이그레이션 구조 보존).
curl -s -X POST "http://localhost:9201/feeds/_delete_by_query?conflicts=proceed&refresh=true" \
  -H "Content-Type: application/json" -d '{"query":{"match_all":{}}}' > /dev/null 2>&1 || true
docker compose exec -T postgres psql -U otboo_user -d otboo -t -A -c "
SELECT '{\"index\":{\"_index\":\"feeds\",\"_id\":\"' || id || '\"}}' || chr(10) ||
  json_build_object('id', id, 'content', content, 'authorId', author_id,
    'skyStatus', sky_status, 'precipitationType', precipitation_type,
    'createdAt', to_char(created_at at time zone 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'),
    'likeCount', like_count)::text
FROM feeds WHERE deleted_at IS NULL;" > /tmp/reset_feeds_bulk.ndjson
curl -s -X POST "http://localhost:9201/_bulk" -H "Content-Type: application/x-ndjson" \
  --data-binary "@/tmp/reset_feeds_bulk.ndjson" -o /tmp/reset_bulk_response.json
rm -f /tmp/reset_feeds_bulk.ndjson /tmp/reset_bulk_response.json

echo "reset done: $(docker compose exec -T postgres psql -U otboo_user -d otboo -t -A -c 'SELECT count(*) FROM users;') users, $(docker compose exec -T postgres psql -U otboo_user -d otboo -t -A -c 'SELECT count(*) FROM feeds WHERE deleted_at IS NULL;') active feeds, ES=$(curl -s "http://localhost:9201/feeds/_count" | grep -o '"count":[0-9]*')"
