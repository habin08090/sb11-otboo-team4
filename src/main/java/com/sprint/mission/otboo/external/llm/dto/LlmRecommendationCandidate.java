package com.sprint.mission.otboo.external.llm.dto;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import java.util.UUID;

public record LlmRecommendationCandidate(
    UUID clothesId,
    String name,
    ClothesType type,
    String attributeSummary
) {

}
