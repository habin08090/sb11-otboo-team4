package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ChatbotAnswerFailedException extends ChatbotException {

  private static final HttpStatus STATUS = HttpStatus.BAD_GATEWAY;
  private static final String MESSAGE = "지금은 답변을 드릴 수 없어요. 잠시 후 다시 물어봐 주세요.";

  private ChatbotAnswerFailedException(Map<String, Object> details, Throwable cause) {
    super(STATUS, MESSAGE, details, cause);
  }

  public static ChatbotAnswerFailedException llmCallFailed(UUID userId, Throwable cause) {
    return new ChatbotAnswerFailedException(Map.of("userId", userId), cause);
  }
}
