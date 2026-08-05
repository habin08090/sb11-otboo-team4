package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class WeatherNotFoundException extends RecommendationException {

  private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
  private static final String MESSAGE = "날씨 정보를 찾을 수 없습니다.";

  private WeatherNotFoundException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static WeatherNotFoundException withId(UUID weatherId) {
    return new WeatherNotFoundException(Map.of("weatherId", weatherId));
  }
}