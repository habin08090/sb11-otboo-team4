package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ChatbotUnauthorizedException extends ChatbotException {

  private static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;
  private static final String MESSAGE = "인증 정보가 없습니다.";

  private ChatbotUnauthorizedException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static ChatbotUnauthorizedException withNone() {
    return new ChatbotUnauthorizedException(Map.of());
  }
}
