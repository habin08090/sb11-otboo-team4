package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.controller;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.controller.api.ClothesAttributeDefApi;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefUpdateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.service.ClothesAttributeDefService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

@RequiredArgsConstructor
@RequestMapping("/api/clothes/attribute-defs")
@RestController
public class ClothesAttributeDefController implements ClothesAttributeDefApi {

  private final ClothesAttributeDefService clothesAttributeDefService;

  @Override
  @PostMapping
  public ResponseEntity<ClothesAttributeDefDto> create(
      @Valid @RequestBody ClothesAttributeDefCreateRequest request) {
    ClothesAttributeDefDto created = clothesAttributeDefService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<ClothesAttributeDefDto>> getAll(
      @Valid @ModelAttribute ClothesAttributeDefListParams params) {
    return ResponseEntity.ok(clothesAttributeDefService.getAll(params));
  }

  @Override
  @PatchMapping("/{definitionId}")
  public ResponseEntity<ClothesAttributeDefDto> update(
      @PathVariable UUID definitionId,
      @Valid @RequestBody ClothesAttributeDefUpdateRequest request) {
    return ResponseEntity.ok(clothesAttributeDefService.update(definitionId, request));
  }
  @Override
  @DeleteMapping("/{definitionId}")
  public ResponseEntity<Void> delete(@PathVariable UUID definitionId) {
    clothesAttributeDefService.delete(definitionId);
    return ResponseEntity.noContent().build();
  }
}