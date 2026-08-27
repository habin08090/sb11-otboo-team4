package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 사용자별 분당 질문 횟수를 Redis 카운터로 제한한다.
 *
 * <p>챗봇은 사용자가 자유롭게 반복 호출할 수 있어 LLM 비용이 예측되지 않는다. 카운터를 처음 만들 때 1분 TTL을 걸어 두므로, 만료와 함께 카운터가 사라진다 — 별도의
 * 초기화나 시각 계산이 필요 없다.
 */
@Slf4j
@Component
public class ChatbotRateLimiter {

  private static final String KEY_PREFIX = "chatbot:ratelimit:";
  private static final Duration WINDOW = Duration.ofMinutes(1);

  private final StringRedisTemplate redisTemplate;
  private final int limitPerMinute;

  public ChatbotRateLimiter(StringRedisTemplate redisTemplate,
      @Value("${chatbot.rate-limit.per-minute:10}") int limitPerMinute) {
    this.redisTemplate = redisTemplate;
    this.limitPerMinute = limitPerMinute;
  }

  public void check(UUID userId) {
    Long count;
    try {
      String key = key(userId);
      count = redisTemplate.opsForValue().increment(key);
      if (count != null && count == 1L) {
        redisTemplate.expire(key, WINDOW);
      }
    } catch (DataAccessException e) {
      // Redis 장애로 챗봇 자체가 멈추는 것보다, 제한 없이 답하는 편이 낫다고 보고 통과시킨다.
      log.warn("챗봇 사용량 카운터 조회 실패, 제한 없이 진행한다", e);
      return;
    }

    if (count != null && count > limitPerMinute) {
      throw ChatbotRateLimitExceededException.withUserId(userId, limitPerMinute);
    }
  }

  private String key(UUID userId) {
    return KEY_PREFIX + userId;
  }
}
