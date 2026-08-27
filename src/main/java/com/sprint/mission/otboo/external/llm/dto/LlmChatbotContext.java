package com.sprint.mission.otboo.external.llm.dto;

import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import java.util.List;

public record LlmChatbotContext(
    String question,
    double temperature,
    PrecipitationType precipitationType,
    WindStrength windStrength,
    int sensitivity,
    List<LlmChatbotWardrobeItem> wardrobe
) {

}
