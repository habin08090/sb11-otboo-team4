package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class ChatbotRateLimiterTest {

  private static final int LIMIT_PER_MINUTE = 5;

  @Mock
  StringRedisTemplate redisTemplate;

  ChatbotRateLimiter chatbotRateLimiter;

  @BeforeEach
  void setUp() {
    chatbotRateLimiter = new ChatbotRateLimiter(redisTemplate, LIMIT_PER_MINUTE);
  }

  @SuppressWarnings("unchecked")
  private void givenCount(long count) {
    given(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
        .willReturn(count);
  }

  @Nested
  @DisplayName("사용량 검사")
  class Check {

    @Test
    @DisplayName("제한_이내면_통과한다")
    void 제한_이내면_통과한다() {
      // given
      UUID userId = UUID.randomUUID();
      givenCount(1L);

      // when & then
      assertThatCode(() -> chatbotRateLimiter.check(userId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("증가와_만료를_하나의_스크립트로_실행한다")
    @SuppressWarnings("unchecked")
    void 증가와_만료를_하나의_스크립트로_실행한다() {
      // given
      UUID userId = UUID.randomUUID();
      givenCount(1L);

      // when
      chatbotRateLimiter.check(userId);

      // then
      ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
      ArgumentCaptor<String> ttlCaptor = ArgumentCaptor.forClass(String.class);
      verify(redisTemplate)
          .execute(any(RedisScript.class), keysCaptor.capture(), ttlCaptor.capture());

      assertThat(keysCaptor.getValue()).containsExactly("chatbot:ratelimit:" + userId);
      assertThat(ttlCaptor.getValue())
          .isEqualTo(String.valueOf(Duration.ofMinutes(1).toMillis()));
    }

    @Test
    @DisplayName("제한을_초과하면_ChatbotRateLimitExceededException을_던진다")
    void 제한을_초과하면_ChatbotRateLimitExceededException을_던진다() {
      // given
      UUID userId = UUID.randomUUID();
      givenCount(LIMIT_PER_MINUTE + 1);

      // when & then
      assertThatThrownBy(() -> chatbotRateLimiter.check(userId))
          .isInstanceOf(ChatbotRateLimitExceededException.class);
    }

    @Test
    @DisplayName("제한과_같은_횟수까지는_통과한다")
    void 제한과_같은_횟수까지는_통과한다() {
      // given
      UUID userId = UUID.randomUUID();
      givenCount(LIMIT_PER_MINUTE);

      // when & then
      assertThatCode(() -> chatbotRateLimiter.check(userId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis가_실패하면_제한을_적용하지_않고_통과시킨다")
    @SuppressWarnings("unchecked")
    void Redis가_실패하면_제한을_적용하지_않고_통과시킨다() {
      // given
      UUID userId = UUID.randomUUID();
      willThrow(new QueryTimeoutException("redis down"))
          .given(redisTemplate).execute(any(RedisScript.class), anyList(), anyString());

      // when & then
      assertThatCode(() -> chatbotRateLimiter.check(userId)).doesNotThrowAnyException();
    }
  }
}
