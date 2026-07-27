package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception;

import com.sprint.mission.otboo.global.exception.OtbooException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class AttributeDefException extends OtbooException {

  protected AttributeDefException(HttpStatus status, String message, Map<String, Object> details) {
    super(status, message, details);
  }
}