package com.sprint.mission.otboo.domain.social.feed.exception;

import java.util.Collections;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class AuthorNotFoundException extends FeedException {

  private static final String MESSAGE = "작성자 정보를 찾을 수 없습니다.";

  private AuthorNotFoundException(Map<String, Object> details) {
    super(HttpStatus.NOT_FOUND, MESSAGE, details);
  }

  public static AuthorNotFoundException withNone() {
    return new AuthorNotFoundException(Collections.emptyMap());
  }
}