package com.sprint.mission.otboo.domain.social.directmessage.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class DirectMessageUnauthorizedException extends DirectMessageException {

  private static final String MESSAGE = "인증 정보를 확인할 수 없습니다.";

  private DirectMessageUnauthorizedException(Map<String, Object> details) {
    super(HttpStatus.UNAUTHORIZED, MESSAGE, details);
  }

  public static DirectMessageUnauthorizedException withNone() {
    return new DirectMessageUnauthorizedException(Map.of());
  }
}
