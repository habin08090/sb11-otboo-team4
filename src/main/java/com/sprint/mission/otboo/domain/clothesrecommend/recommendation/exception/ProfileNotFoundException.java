package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends RecommendationException {

  private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
  private static final String MESSAGE = "프로필 정보를 찾을 수 없습니다.";

  private ProfileNotFoundException() {
    super(STATUS, MESSAGE, Map.of());
  }

  public static ProfileNotFoundException withNone() {
    return new ProfileNotFoundException();
  }
}