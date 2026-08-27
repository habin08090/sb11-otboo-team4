package com.sprint.mission.otboo.external.llm.dto;

import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;

public record LlmChatbotWeather(
    double temperature,
    PrecipitationType precipitationType,
    WindStrength windStrength
) {

}
