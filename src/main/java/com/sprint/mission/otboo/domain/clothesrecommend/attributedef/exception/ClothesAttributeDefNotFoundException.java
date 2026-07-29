package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ClothesAttributeDefNotFoundException extends AttributeDefException {

  private ClothesAttributeDefNotFoundException(Map<String, Object> details) {
    super(
        HttpStatus.NOT_FOUND,
        AttributeDefErrorCode.ATTRIBUTE_DEF_NOT_FOUND.getMessage(),
        details);
  }

  public static ClothesAttributeDefNotFoundException withId(UUID definitionId) {
    return new ClothesAttributeDefNotFoundException(Map.of("definitionId", definitionId));
  }
}