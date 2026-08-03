package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record ClothesListParams(
    String cursor,
    UUID idAfter,
    @NotNull @Min(1) @Max(100) Integer limit,
    ClothesType typeEqual,
    @NotNull UUID ownerId
) {

  @AssertTrue(message = "cursor, idAfter는 함께 전달되어야 합니다")
  public boolean isCursorAndIdAfterConsistent() {
    return (cursor == null && idAfter == null) || (cursor != null && idAfter != null);
  }

  @AssertTrue(message = "cursor는 유효한 Instant 형식이어야 합니다")
  public boolean isCursorFormatValid() {
    if (cursor == null) {
      return true;
    }
    try {
      Instant.parse(cursor);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}