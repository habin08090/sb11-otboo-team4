package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends RecommendationException {

  private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
  private static final String MESSAGE = "프로필 정보를 찾을 수 없습니다.";

  private ProfileNotFoundException(Map<String, Object> details) {
    super(STATUS, MESSAGE, details);
  }

  public static ProfileNotFoundException withUserId(UUID userId) {
    return new ProfileNotFoundException(Map.of("userId", userId));
  }
}