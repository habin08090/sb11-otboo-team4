package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception;

import com.sprint.mission.otboo.global.exception.OtbooException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class ChatbotException extends OtbooException {

  protected ChatbotException(HttpStatus status, String message, Map<String, Object> details) {
    super(status, message, details);
  }

  protected ChatbotException(HttpStatus status, String message, Map<String, Object> details,
      Throwable cause) {
    super(status, message, details, cause);
  }
}
