-- chat_conversation.login_id -> app_user.login_id FK 보강 스크립트
-- PostgreSQL 기준.

BEGIN;

-- FK 추가 전 고아 대화 레코드 정리
DELETE FROM chat_conversation c
WHERE NOT EXISTS (
    SELECT 1
    FROM app_user u
    WHERE u.login_id = c.login_id
);

-- login_id 조회 성능 보강
CREATE INDEX IF NOT EXISTS idx_chat_conversation_login_id
    ON chat_conversation (login_id);

-- FK 제약 추가(중복 실행 안전)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_chat_conversation_user'
    ) THEN
        ALTER TABLE chat_conversation
            ADD CONSTRAINT fk_chat_conversation_user
            FOREIGN KEY (login_id)
            REFERENCES app_user(login_id)
            ON UPDATE CASCADE
            ON DELETE CASCADE;
    END IF;
END
$$;

COMMIT;
