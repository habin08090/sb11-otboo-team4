package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.controller;

import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.controller.api.RecommendationApi;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationParams;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service.RecommendationService;
import com.sprint.mission.otboo.security.details.CurrentUser;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
@RestController
public class RecommendationController implements RecommendationApi {

  private final RecommendationService recommendationService;

  @Override
  @GetMapping
  public ResponseEntity<RecommendationDto> getRecommendation(
      @Valid RecommendationParams params,
      @CurrentUser UserPrincipal principal) {
    RecommendationDto result = recommendationService.recommend(
        params.weatherId(), principal.userId());
    return ResponseEntity.ok(result);
  }
}