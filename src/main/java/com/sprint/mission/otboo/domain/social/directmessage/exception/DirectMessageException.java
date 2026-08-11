package com.sprint.mission.otboo.domain.social.directmessage.exception;

import com.sprint.mission.otboo.global.exception.OtbooException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class DirectMessageException extends OtbooException {

  protected DirectMessageException(HttpStatus status, String message, Map<String, Object> details) {
    super(status, message, details);
  }
}