package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ClothesAttributeDefNameDuplicatedException extends AttributeDefException {

  private ClothesAttributeDefNameDuplicatedException(Map<String, Object> details) {
    super(
        HttpStatus.CONFLICT,
        AttributeDefErrorCode.ATTRIBUTE_DEF_NAME_DUPLICATED.getMessage(),
        details);
  }

  public static ClothesAttributeDefNameDuplicatedException withName(String name) {
    return new ClothesAttributeDefNameDuplicatedException(Map.of("name", name));
  }
}