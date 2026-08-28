package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 사용자별 분당 질문 횟수를 Redis 카운터로 제한한다.
 *
 * <p>챗봇은 사용자가 자유롭게 반복 호출할 수 있어 LLM 비용이 예측되지 않는다.
 *
 * <p>증가와 만료 설정을 Lua 스크립트로 묶어 한 번에 처리한다. {@code INCR}은 TTL을 만들지 않으므로 따로 {@code EXPIRE}를 불러야 하는데, 그
 * 사이에 실패하면 키가 만료 없이 남아 그 사용자가 영영 차단된다.
 */
@Slf4j
@Component
public class ChatbotRateLimiter {

  private static final String KEY_PREFIX = "chatbot:ratelimit:";
  private static final Duration WINDOW = Duration.ofMinutes(1);

  /** 카운터를 올리고, 새로 만들어진 키에만 만료를 건다. 반환값은 증가 후 횟수. */
  private static final RedisScript<Long> INCREMENT_WITH_EXPIRE = new DefaultRedisScript<>("""
      local count = redis.call('INCR', KEYS[1])
      if count == 1 then
        redis.call('PEXPIRE', KEYS[1], ARGV[1])
      end
      return count
      """, Long.class);

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
      count = redisTemplate.execute(INCREMENT_WITH_EXPIRE, List.of(key(userId)),
          String.valueOf(WINDOW.toMillis()));
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
