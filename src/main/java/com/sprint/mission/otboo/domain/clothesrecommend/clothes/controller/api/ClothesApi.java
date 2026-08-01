package com.sprint.mission.otboo.domain.clothesrecommend.clothes.controller.api;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesListParams;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import com.sprint.mission.otboo.global.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "의상 관리", description = "의상 관련 API")
public interface ClothesApi {

  @Operation(summary = "옷 목록 조회", description = "옷 목록 조회 API")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "옷 목록 조회 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "옷 목록 조회 실패",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<CursorPageResponse<ClothesDto>> getClothes(
      @ParameterObject ClothesListParams params);

  @Operation(summary = "옷 등록", description = "옷 등록 API")
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "옷 등록 성공",
          content = @Content(
              schema = @Schema(implementation = ClothesDto.class))),
      @ApiResponse(
          responseCode = "400",
          description = "옷 등록 실패",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ClothesDto> create(ClothesCreateRequest request, MultipartFile image);
}