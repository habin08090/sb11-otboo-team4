package com.sprint.mission.otboo.domain.social.feed.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class FeedForbiddenException extends FeedException {

  private static final String MESSAGE = "본인만 수행할 수 있습니다.";

  private FeedForbiddenException(Map<String, Object> details) {
    super(HttpStatus.FORBIDDEN, MESSAGE, details);
  }

  public static FeedForbiddenException authorMismatch(UUID current, UUID requested) {
    return new FeedForbiddenException(
        Map.of("currentUserId", current, "requestedAuthorId", requested));
  }
}