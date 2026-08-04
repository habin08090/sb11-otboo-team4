package com.sprint.mission.otboo.domain.social.feed.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class WeatherNotFoundException extends FeedException {

  private static final String MESSAGE = "날씨 정보를 찾을 수 없습니다.";

  private WeatherNotFoundException(Map<String, Object> details) {
    super(HttpStatus.BAD_REQUEST, MESSAGE, details);
  }

  public static WeatherNotFoundException withId(UUID weatherId) {
    return new WeatherNotFoundException(Map.of("weatherId", weatherId));
  }
}