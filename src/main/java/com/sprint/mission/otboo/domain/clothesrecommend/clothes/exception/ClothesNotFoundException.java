package com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ClothesNotFoundException extends ClothesException {

  private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
  private static final String MESSAGE = "의상을 찾을 수 없습니다.";

  private ClothesNotFoundException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static ClothesNotFoundException withId(UUID clothesId) {
    return new ClothesNotFoundException(Map.of("clothesId", clothesId));
  }
}