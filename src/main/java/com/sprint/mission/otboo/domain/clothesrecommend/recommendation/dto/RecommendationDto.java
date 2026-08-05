package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto;

import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import java.util.List;
import java.util.UUID;

public record RecommendationDto(
    UUID weatherId,
    UUID userId,
    List<OotdDto> clothes
) {

}