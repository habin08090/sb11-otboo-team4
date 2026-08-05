package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.ClothesAttribute;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesAttributeRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.ProfileNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.SkyStatus;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

  @InjectMocks
  RecommendationService recommendationService;

  @Mock
  WeatherRepository weatherRepository;

  @Mock
  ProfileRepository profileRepository;

  @Mock
  ClothesRepository clothesRepository;

  @Mock
  ClothesAttributeRepository clothesAttributeRepository;



  private Weather createWeather(double temperature, PrecipitationType precipitationType,
      SkyStatus skyStatus, WindStrength windStrength) {
    Weather w = Weather.create(
        null, null, null,
        skyStatus, precipitationType, 0, 0,
        0, 0,
        temperature, 0, temperature - 3, temperature + 3,
        windStrength == WindStrength.STRONG ? 15.0 : 3.0,
        windStrength
    );
    ReflectionTestUtils.setField(w, "id", UUID.randomUUID());
    return w;
  }

  private Profile createProfile(UUID userId, int temperatureSensitivity) {
    Profile profile = Profile.createDefault(null);
    ReflectionTestUtils.setField(profile, "id", userId);
    ReflectionTestUtils.setField(profile, "temperatureSensitivity", temperatureSensitivity);
    return profile;
  }

  private Clothes createClothes(UUID ownerId, String name, ClothesType type) {
    Clothes clothes = Clothes.create(ownerId, name, type);
    ReflectionTestUtils.setField(clothes, "id", UUID.randomUUID());
    return clothes;
  }



  @Nested
  @DisplayName("추천 조회")
  class 추천_조회 {

    @Test
    @DisplayName("날씨_프로필_의상이_모두_있으면_추천_결과를_반환한다")
    void 날씨_프로필_의상이_모두_있으면_추천_결과를_반환한다() {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      Weather weather = createWeather(25.0, PrecipitationType.NONE,
          SkyStatus.CLEAR, WindStrength.WEAK);
      Profile profile = createProfile(userId, 3);

      Clothes top = createClothes(userId, "반팔 티셔츠", ClothesType.TOP);
      Clothes bottom = createClothes(userId, "반바지", ClothesType.BOTTOM);
      Clothes shoes = createClothes(userId, "운동화", ClothesType.SHOES);

      given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
      given(profileRepository.findByIdWithUser(userId)).willReturn(Optional.of(profile));
      given(clothesRepository.findAllByOwnerIdAndSoftDeletableDeletedAtIsNull(userId))
          .willReturn(List.of(top, bottom, shoes));
      given(clothesAttributeRepository.findAllByClothesIdsWithDefinition(any()))
          .willReturn(List.of());

      // when
      RecommendationDto result = recommendationService.recommend(weatherId, userId);

      // then
      assertThat(result.weatherId()).isEqualTo(weatherId);
      assertThat(result.userId()).isEqualTo(userId);
      assertThat(result.clothes()).isNotEmpty();
    }

    @Test
    @DisplayName("날씨_정보가_없으면_WeatherNotFoundException을_던진다")
    void 날씨_정보가_없으면_WeatherNotFoundException을_던진다() {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      given(weatherRepository.findById(weatherId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> recommendationService.recommend(weatherId, userId))
          .isInstanceOf(WeatherNotFoundException.class);
    }

    @Test
    @DisplayName("프로필_정보가_없으면_ProfileNotFoundException을_던진다")
    void 프로필_정보가_없으면_ProfileNotFoundException을_던진다() {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      Weather weather = createWeather(25.0, PrecipitationType.NONE,
          SkyStatus.CLEAR, WindStrength.WEAK);

      given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
      given(profileRepository.findByIdWithUser(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> recommendationService.recommend(weatherId, userId))
          .isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    @DisplayName("보유_의상이_없으면_빈_리스트를_반환한다")
    void 보유_의상이_없으면_빈_리스트를_반환한다() {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      Weather weather = createWeather(25.0, PrecipitationType.NONE,
          SkyStatus.CLEAR, WindStrength.WEAK);
      Profile profile = createProfile(userId, 3);

      given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
      given(profileRepository.findByIdWithUser(userId)).willReturn(Optional.of(profile));
      given(clothesRepository.findAllByOwnerIdAndSoftDeletableDeletedAtIsNull(userId))
          .willReturn(List.of());

      // when
      RecommendationDto result = recommendationService.recommend(weatherId, userId);

      // then
      assertThat(result.clothes()).isEmpty();
    }

    @Test
    @DisplayName("비가_오면_아우터가_추천에_포함된다")
    void 비가_오면_아우터가_추천에_포함된다() {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      Weather weather = createWeather(25.0, PrecipitationType.RAIN,
          SkyStatus.CLOUDY, WindStrength.WEAK);
      Profile profile = createProfile(userId, 3);

      Clothes top = createClothes(userId, "반팔 티셔츠", ClothesType.TOP);
      Clothes bottom = createClothes(userId, "반바지", ClothesType.BOTTOM);
      Clothes shoes = createClothes(userId, "운동화", ClothesType.SHOES);
      Clothes outer = createClothes(userId, "바람막이", ClothesType.OUTER);

      given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
      given(profileRepository.findByIdWithUser(userId)).willReturn(Optional.of(profile));
      given(clothesRepository.findAllByOwnerIdAndSoftDeletableDeletedAtIsNull(userId))
          .willReturn(List.of(top, bottom, shoes, outer));
      given(clothesAttributeRepository.findAllByClothesIdsWithDefinition(any()))
          .willReturn(List.of());

      // when
      RecommendationDto result = recommendationService.recommend(weatherId, userId);

      // then
      assertThat(result.clothes())
          .anyMatch(ootd -> ootd.type() == ClothesType.OUTER);
    }

    @Test
    @DisplayName("추위_민감도가_높으면_더_따뜻한_옷이_추천된다")
    void 추위_민감도가_높으면_더_따뜻한_옷이_추천된다() {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID userId = UUID.randomUUID();

      Weather weather = createWeather(17.0, PrecipitationType.NONE,
          SkyStatus.CLEAR, WindStrength.WEAK);
      Profile profile = createProfile(userId, 1);

      Clothes top = createClothes(userId, "긴팔 셔츠", ClothesType.TOP);
      Clothes bottom = createClothes(userId, "긴바지", ClothesType.BOTTOM);
      Clothes shoes = createClothes(userId, "부츠", ClothesType.SHOES);
      Clothes outer = createClothes(userId, "자켓", ClothesType.OUTER);

      given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
      given(profileRepository.findByIdWithUser(userId)).willReturn(Optional.of(profile));
      given(clothesRepository.findAllByOwnerIdAndSoftDeletableDeletedAtIsNull(userId))
          .willReturn(List.of(top, bottom, shoes, outer));
      given(clothesAttributeRepository.findAllByClothesIdsWithDefinition(any()))
          .willReturn(List.of());

      // when
      RecommendationDto result = recommendationService.recommend(weatherId, userId);

      // then
      assertThat(result.clothes())
          .anyMatch(ootd -> ootd.type() == ClothesType.OUTER);
    }
  }
}