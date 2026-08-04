package com.sprint.mission.otboo.domain.social.feed.dto;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesAttributeWithDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import java.util.List;
import java.util.UUID;

public record OotdSnapshot(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}