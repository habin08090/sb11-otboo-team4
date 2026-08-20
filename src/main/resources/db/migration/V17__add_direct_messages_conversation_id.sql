-- 양방향 OR 조건 (sender=A AND receiver=B) OR (sender=B AND receiver=A) 은
-- BitmapOr 로 합쳐지면서 인덱스의 정렬 순서를 잃어 매 페이지 Sort 가 발생한다.
-- 두 사용자 UUID 를 사전순으로 결합한 conversation_id 를 두면 조회가 단일 등치가 되어
-- (conversation_id, created_at DESC, id DESC) 인덱스 하나로 탐색과 정렬을 모두 커버한다.

ALTER TABLE direct_messages ADD COLUMN conversation_id VARCHAR(73);

UPDATE direct_messages
SET conversation_id = CASE
                          WHEN sender_id::text < receiver_id::text
        THEN sender_id::text || '_' || receiver_id::text
                          ELSE receiver_id::text || '_' || sender_id::text
    END;

ALTER TABLE direct_messages ALTER COLUMN conversation_id SET NOT NULL;

DROP INDEX IF EXISTS IDX_direct_messages_sender_receiver_created_id;

CREATE INDEX IDX_direct_messages_conversation_created_id
    ON direct_messages (conversation_id, created_at DESC, id DESC);
