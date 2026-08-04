package com.sprint.mission.otboo.domain.social.feed.service;

import com.sprint.mission.otboo.domain.social.feed.dto.WeatherSnapshot;
import com.sprint.mission.otboo.domain.social.feed.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class WeatherSnapshotProvider {

  private final WeatherRepository weatherRepository;

  public WeatherSnapshot readSnapshot(UUID weatherId) {
    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> {
          log.warn("날씨 조회 실패: weatherId={}", weatherId);
          return WeatherNotFoundException.withId(weatherId);
        });

    return new WeatherSnapshot(
        weather.getSkyStatus(),
        weather.getPrecipitationType(),
        weather.getPrecipitationAmount(),
        weather.getPrecipitationProbability(),
        weather.getTemperatureCurrent(),
        weather.getTemperatureCompared(),
        weather.getTemperatureMin(),
        weather.getTemperatureMax()
    );
  }
}