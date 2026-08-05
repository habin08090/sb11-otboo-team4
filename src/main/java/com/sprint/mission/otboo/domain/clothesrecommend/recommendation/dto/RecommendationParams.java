package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RecommendationParams(
    @NotNull UUID weatherId
) {

}