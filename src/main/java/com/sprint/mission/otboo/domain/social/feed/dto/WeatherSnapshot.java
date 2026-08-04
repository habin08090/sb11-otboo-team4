package com.sprint.mission.otboo.domain.social.feed.dto;

import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.SkyStatus;

public record WeatherSnapshot(
    SkyStatus skyStatus,
    PrecipitationType precipitationType,
    Double precipitationAmount,
    Double precipitationProbability,
    Double temperatureCurrent,
    Double temperatureCompared,
    Double temperatureMin,
    Double temperatureMax
) {

}