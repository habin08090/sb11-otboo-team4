package com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ClothesExtractionBadRequestException extends ClothesException {

  private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

  private ClothesExtractionBadRequestException(String message, Map<String, Object> details) {
    super(STATUS, message, details);
  }

  public static ClothesExtractionBadRequestException invalidUrl(String url) {
    return new ClothesExtractionBadRequestException(
        "유효하지 않은 URL입니다.",
        Map.of("url", url != null ? url : "")
    );
  }
}
