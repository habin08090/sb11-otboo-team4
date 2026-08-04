package com.sprint.mission.otboo.domain.social.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.sprint.mission.otboo.domain.social.feed.dto.WeatherSnapshot;
import com.sprint.mission.otboo.domain.social.feed.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.SkyStatus;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherSnapshotProvider")
class WeatherSnapshotProviderTest {

  static final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
      .build();

  @InjectMocks
  WeatherSnapshotProvider weatherSnapshotProvider;

  @Mock
  WeatherRepository weatherRepository;

  @Nested
  @DisplayName("readSnapshot")
  class ReadSnapshot {

    @Test
    @DisplayName("존재하는 weatherId면 WeatherSnapshot을 반환한다")
    void 존재하는_weatherId면_WeatherSnapshot을_반환한다() {
      // given
      UUID weatherId = UUID.randomUUID();
      Weather weather = fm.giveMeBuilder(Weather.class)
          .set("skyStatus", SkyStatus.CLEAR)
          .set("precipitationType", PrecipitationType.NONE)
          .set("precipitationAmount", 0.0)
          .set("precipitationProbability", 0.0)
          .set("temperatureCurrent", 28.0)
          .set("temperatureCompared", 2.0)
          .set("temperatureMin", 16.0)
          .set("temperatureMax", 31.0)
          .sample();
      when(weatherRepository.findById(weatherId)).thenReturn(Optional.of(weather));

      // when
      WeatherSnapshot result = weatherSnapshotProvider.readSnapshot(weatherId);

      // then
      assertThat(result.skyStatus()).isEqualTo(SkyStatus.CLEAR);
      assertThat(result.precipitationType()).isEqualTo(PrecipitationType.NONE);
      assertThat(result.temperatureCurrent()).isEqualTo(28.0);
      assertThat(result.temperatureCompared()).isEqualTo(2.0);
      assertThat(result.temperatureMin()).isEqualTo(16.0);
      assertThat(result.temperatureMax()).isEqualTo(31.0);
    }

    @Test
    @DisplayName("존재하지 않는 weatherId면 WeatherNotFoundException을 던진다")
    void 존재하지_않는_weatherId면_WeatherNotFoundException을_던진다() {
      // given
      UUID weatherId = UUID.randomUUID();
      when(weatherRepository.findById(weatherId)).thenReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> weatherSnapshotProvider.readSnapshot(weatherId))
          .isInstanceOf(WeatherNotFoundException.class)
          .satisfies(ex -> {
            WeatherNotFoundException e = (WeatherNotFoundException) ex;
            assertThat(e.getStatus().value()).isEqualTo(400);
            assertThat(e.getDetails()).containsEntry("weatherId", weatherId);
          });
    }
  }
}