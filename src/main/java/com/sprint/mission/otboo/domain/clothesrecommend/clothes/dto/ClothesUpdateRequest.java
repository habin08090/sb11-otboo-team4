package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import java.util.List;

public record ClothesUpdateRequest(
    String name,
    ClothesType type,
    List<ClothesAttributeDto> attributes
) {
  // 전부 nullable — 부분 수정(partial update)
}