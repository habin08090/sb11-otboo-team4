package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ClothesAttributeDefNotFoundException extends AttributeDefException {

  private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
  private static final String MESSAGE = "의상 속성 정의를 찾을 수 없습니다.";

  private ClothesAttributeDefNotFoundException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static ClothesAttributeDefNotFoundException withId(UUID definitionId) {
    return new ClothesAttributeDefNotFoundException(Map.of("definitionId", definitionId));
  }
}