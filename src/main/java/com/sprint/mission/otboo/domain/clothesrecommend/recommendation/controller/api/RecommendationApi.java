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

  @Operation(summary = "추천 조회", description = "추천 조회 API. 추천 가능한 의상이 없으면 clothes 필드가 빈 배열로 반환됩니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "추천 조회 성공 (추천 가능한 의상이 없으면 빈 배열)",
          content = @Content(
              schema = @Schema(implementation = RecommendationDto.class))),
      @ApiResponse(
          responseCode = "400",
          description = "weatherId 누락 또는 UUID 형식 오류",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "404",
          description = "날씨 또는 프로필 정보를 찾을 수 없음",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<RecommendationDto> getRecommendation(
      @ParameterObject RecommendationParams params,
      UserPrincipal principal);
}