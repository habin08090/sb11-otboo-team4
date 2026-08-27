package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ChatbotRateLimiterTest {

  private static final int LIMIT_PER_MINUTE = 5;

  @Mock
  StringRedisTemplate redisTemplate;
  @Mock
  ValueOperations<String, String> valueOperations;

  ChatbotRateLimiter chatbotRateLimiter;

  @BeforeEach
  void setUp() {
    chatbotRateLimiter = new ChatbotRateLimiter(redisTemplate, LIMIT_PER_MINUTE);
  }

  @Nested
  @DisplayName("사용량 검사")
  class Check {

    @Test
    @DisplayName("제한_이내면_통과하고_카운터를_증가시킨다")
    void 제한_이내면_통과하고_카운터를_증가시킨다() {
      // given
      UUID userId = UUID.randomUUID();
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.increment(anyString())).willReturn(1L);

      // when & then
      assertThatCode(() -> chatbotRateLimiter.check(userId)).doesNotThrowAnyException();
      verify(valueOperations).increment(anyString());
    }

    @Test
    @DisplayName("첫_호출이면_카운터에_만료시간을_건다")
    void 첫_호출이면_카운터에_만료시간을_건다() {
      // given
      UUID userId = UUID.randomUUID();
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.increment(anyString())).willReturn(1L);

      // when
      chatbotRateLimiter.check(userId);

      // then
      verify(redisTemplate).expire(anyString(), eq(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("두_번째_호출부터는_만료시간을_다시_걸지_않는다")
    void 두_번째_호출부터는_만료시간을_다시_걸지_않는다() {
      // given
      UUID userId = UUID.randomUUID();
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.increment(anyString())).willReturn(2L);

      // when
      chatbotRateLimiter.check(userId);

      // then
      verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("제한을_초과하면_ChatbotRateLimitExceededException을_던진다")
    void 제한을_초과하면_ChatbotRateLimitExceededException을_던진다() {
      // given
      UUID userId = UUID.randomUUID();
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.increment(anyString())).willReturn((long) LIMIT_PER_MINUTE + 1);

      // when & then
      assertThatThrownBy(() -> chatbotRateLimiter.check(userId))
          .isInstanceOf(ChatbotRateLimitExceededException.class);
    }

    @Test
    @DisplayName("제한과_같은_횟수까지는_통과한다")
    void 제한과_같은_횟수까지는_통과한다() {
      // given
      UUID userId = UUID.randomUUID();
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.increment(anyString())).willReturn((long) LIMIT_PER_MINUTE);

      // when & then
      assertThatCode(() -> chatbotRateLimiter.check(userId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis가_실패하면_제한을_적용하지_않고_통과시킨다")
    void Redis가_실패하면_제한을_적용하지_않고_통과시킨다() {
      // given
      UUID userId = UUID.randomUUID();
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      willThrow(new QueryTimeoutException("redis down"))
          .given(valueOperations).increment(anyString());

      // when & then
      assertThatCode(() -> chatbotRateLimiter.check(userId)).doesNotThrowAnyException();
    }
  }
}
