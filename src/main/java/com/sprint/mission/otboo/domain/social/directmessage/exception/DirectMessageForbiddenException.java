package com.sprint.mission.otboo.domain.social.directmessage.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class DirectMessageForbiddenException extends DirectMessageException {

  private static final String MESSAGE = "본인만 수행할 수 있습니다.";

  private DirectMessageForbiddenException(Map<String, Object> details) {
    super(HttpStatus.FORBIDDEN, MESSAGE, details);
  }

  public static DirectMessageForbiddenException senderMismatch() {
    return new DirectMessageForbiddenException(Map.of());
  }
}