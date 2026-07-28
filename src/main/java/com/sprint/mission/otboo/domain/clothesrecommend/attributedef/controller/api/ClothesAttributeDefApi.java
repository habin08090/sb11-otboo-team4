package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.controller.api;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefListParams;
import com.sprint.mission.otboo.global.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "의상 속성 정의", description = "의상 속성 정의 관련 API")
public interface ClothesAttributeDefApi {

  @Operation(summary = "의상 속성 정의 등록", description = "의상 속성 정의 등록 API")
  @ApiResponses({
      @ApiResponse(
          responseCode = "201",
          description = "의상 속성 정의 등록 성공",
          content = @Content(
              schema = @Schema(implementation = ClothesAttributeDefDto.class))),
      @ApiResponse(
          responseCode = "400",
          description = "의상 속성 정의 등록 실패",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(
          responseCode = "409",
          description = "의상 속성 정의 이름 중복",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<ClothesAttributeDefDto> create(ClothesAttributeDefCreateRequest request);

  @Operation(summary = "의상 속성 정의 목록 조회", description = "의상 속성 정의 목록 조회 API")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "의상 속성 정의 목록 조회 성공",
          content = @Content(
              array = @ArraySchema(
                  schema = @Schema(implementation = ClothesAttributeDefDto.class)))),
      @ApiResponse(
          responseCode = "400",
          description = "의상 속성 정의 목록 조회 실패",
          content = @Content(
              schema = @Schema(implementation = ErrorResponse.class)))
  })
  ResponseEntity<List<ClothesAttributeDefDto>> getAll(
      @ParameterObject ClothesAttributeDefListParams params);
}