package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception;

import com.sprint.mission.otboo.global.exception.OtbooException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public abstract class RecommendationException extends OtbooException {

  protected RecommendationException(HttpStatus status, String message,
      Map<String, Object> details) {
    super(status, message, details);
  }
}