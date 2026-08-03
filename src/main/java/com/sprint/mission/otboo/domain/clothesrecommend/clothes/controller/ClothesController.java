package com.sprint.mission.otboo.domain.clothesrecommend.clothes.controller;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.controller.api.ClothesApi;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesService;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RequestMapping("/api/clothes")
@RestController
public class ClothesController implements ClothesApi {

  private final ClothesService clothesService;

  @Override
  @GetMapping
  public ResponseEntity<CursorPageResponse<ClothesDto>> getClothes(
      @Valid ClothesListParams params) {
    // TODO: SecurityContext에서 인증된 사용자 ID를 추출하여 ownerId 검증 (JWT 통합 후 구현)
    return ResponseEntity.ok(clothesService.getClothes(params));
  }

  @Override
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ClothesDto> create(
      @RequestPart @Valid ClothesCreateRequest request,
      @RequestPart(required = false) MultipartFile image) {
    // TODO: SecurityContext에서 인증된 사용자 ID를 추출하여 ownerId 검증 (JWT 통합 후 구현)
    ClothesDto created = clothesService.create(request, image);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}