-- 피드 댓글 목록은 feed_id 등등 조건에 createdAt DESC + id DESC 커서 정렬을 사용한다.
-- V1의 (feed_id) 인덱스는 정렬을 커버하지 못해 해당 피드의 댓글을 전부 읽어 정렬한 뒤 limit을 적용한다.
-- 복합 인덱스로 바꾸면 정렬된 순서로 limit개만 읽고 멈추며,
-- V1의 (feed_id) 인덱스는 이 인덱스의 접두사라 중복이므로 제거한다.
DROP INDEX IF EXISTS IDX_comments_feed_id;
CREATE INDEX IDX_comments_feed_id_created_id
    ON comments (feed_id, created_at DESC, id DESC);
