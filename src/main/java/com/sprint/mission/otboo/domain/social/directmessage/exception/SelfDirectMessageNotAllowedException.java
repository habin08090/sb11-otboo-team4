package com.sprint.mission.otboo.domain.social.directmessage.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class SelfDirectMessageNotAllowedException extends DirectMessageException {

  private static final String MESSAGE = "자기 자신에게는 메시지를 보낼 수 없습니다.";

  private SelfDirectMessageNotAllowedException(Map<String, Object> details) {
    super(HttpStatus.BAD_REQUEST, MESSAGE, details);
  }

  public static SelfDirectMessageNotAllowedException withNone() {
    return new SelfDirectMessageNotAllowedException(Map.of());
  }
}