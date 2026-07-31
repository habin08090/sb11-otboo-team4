package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ClothesAttributeDefNameDuplicatedException extends AttributeDefException {

  private static final HttpStatus STATUS = HttpStatus.CONFLICT;
  private static final String MESSAGE = "이미 존재하는 의상 속성 정의 이름입니다.";

  private ClothesAttributeDefNameDuplicatedException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static ClothesAttributeDefNameDuplicatedException withName(String name) {
    return new ClothesAttributeDefNameDuplicatedException(Map.of("name", name));
  }
}