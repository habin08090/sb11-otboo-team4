package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ChatbotRateLimitExceededException extends ChatbotException {

  private static final HttpStatus STATUS = HttpStatus.TOO_MANY_REQUESTS;
  private static final String MESSAGE = "질문이 너무 잦습니다. 잠시 후 다시 시도해 주세요.";

  private ChatbotRateLimitExceededException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static ChatbotRateLimitExceededException withUserId(UUID userId, int limitPerMinute) {
    return new ChatbotRateLimitExceededException(
        Map.of("userId", userId, "limitPerMinute", limitPerMinute));
  }
}
