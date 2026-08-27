package com.sprint.mission.otboo.external.llm.dto;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;

public record LlmChatbotWardrobeItem(
    String name,
    ClothesType type,
    String attributeSummary
) {

}
