package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.controller.api;

import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationParams;
import com.sprint.mission.otboo.global.dto.ErrorResponse;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "추천 관리", description = "추천 관련 API")
public interface RecommendationApi {

  @Operation(summary = "추천 조회", description = "추천 조회 API")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "추천 조회 성공",
          content = @Content(
              schema = @Schema(implementation = RecommendationDto.class))),
      @ApiResponse(
          responseCode = "400",
          description = "추천 조회 실패",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<RecommendationDto> getRecommendation(
      @ParameterObject RecommendationParams params,
      UserPrincipal principal);
}