package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import java.util.List;
import java.util.UUID;

public record ClothesDto(
    UUID id,
    UUID ownerId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {}