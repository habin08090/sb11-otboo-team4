-- AI 챗봇 계정.
-- 챗봇은 기존 DM 기능(전송·저장·히스토리·인가)을 그대로 재사용하므로 실제 사용자 행이어야 한다.
-- direct_messages.sender_id/receiver_id가 users를 참조하는 FK라, 이 행이 없으면 대화가 저장되지 않는다.
--
-- password는 BCrypt 형식을 지키되 어떤 입력과도 매칭되지 않는 값이다.
-- 이 프로젝트는 DelegatingPasswordEncoder를 쓰므로 '{bcrypt}' 접두어가 필요하고, 뒤이어 오는 60자도
-- BCrypt 규격(salt 22자 + hash 31자, 문자셋 [./A-Za-z0-9])을 지켜야 한다. 규격을 벗어나면 인증 시도가
-- IllegalArgumentException으로 터져 500이 되고, 이 계정만 다른 응답을 내보내 존재가 드러난다.
-- 규격을 지킨 덕에 일반 계정과 똑같이 401(InvalidCredentialsException)로 끝난다.
-- '{noop}'은 평문 인코더라 그 문자열 자체로 로그인될 여지가 있어 쓰지 않았다.
--
-- profiles/follows는 넣지 않는다. 프로필 화면을 거치지 않고, DM에 팔로우 조건도 없다.
INSERT INTO users (id, email, password, name, role, is_locked, lock_reason, created_at, updated_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'chatbot@otboo.local',
        '{bcrypt}$2a$10$CHATBOTACCOUNTNOLOGIN.NoLoginAllowedForThisAccount123',
        'AI 챗봇',
        'USER',
        false,
        'NONE',
        now(),
        now())
ON CONFLICT (id) DO NOTHING;
