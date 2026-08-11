package com.sprint.mission.otboo.domain.social.common.dto;

import jakarta.validation.constraints.AssertTrue;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.springframework.util.StringUtils;

public interface CursorListParams {

  String cursor();

  UUID idAfter();

  @AssertTrue(message = "cursor, idAfter는 함께 전달되어야 합니다")
  default boolean isCursorAndIdAfterConsistent() {
    return StringUtils.hasText(cursor()) == (idAfter() != null);
  }

  @AssertTrue(message = "cursor는 Instant 형식이어야 합니다")
  default boolean isCursorFormatValid() {
    if (!StringUtils.hasText(cursor())) {
      return true;
    }
    try {
      Instant.parse(cursor());
      return true;
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}