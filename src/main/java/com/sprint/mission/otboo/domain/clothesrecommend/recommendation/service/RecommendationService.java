package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.ProfileNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.mapper.RecommendationMapper;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RecommendationService {

  private static final double VERY_COLD_MAX = 4.0;
  private static final double COLD_MAX = 8.0;
  private static final double COOL_MAX = 16.0;
  private static final double HOT_MAX = 27.0;

  private static final double SENSITIVITY_ADJUSTMENT_UNIT = 1.5;
  private static final int SENSITIVITY_CENTER = 3;

  private final WeatherRepository weatherRepository;
  private final ProfileRepository profileRepository;
  private final ClothesRepository clothesRepository;
  private final RecommendationMapper recommendationMapper;

  public RecommendationDto recommend(UUID weatherId, UUID userId) {
    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> WeatherNotFoundException.withId(weatherId));

    Profile profile = profileRepository.findByIdWithUser(userId)
        .orElseThrow(ProfileNotFoundException::withNone);

    List<Clothes> userClothes = clothesRepository
        .findAllByOwnerIdAndSoftDeletableDeletedAtIsNull(userId);

    if (userClothes.isEmpty()) {
      return new RecommendationDto(weatherId, userId, List.of());
    }

    double adjustedTemp = adjustTemperature(
        weather.getTemperatureCurrent(), profile.getTemperatureSensitivity());

    Set<ClothesType> recommendedTypes = getRecommendedTypes(adjustedTemp);

    applyPrecipitationAdjustment(recommendedTypes, weather.getPrecipitationType());
    applyWindAdjustment(recommendedTypes, weather.getWindAsWord());

    List<Clothes> selectedClothes = selectClothesByType(userClothes, recommendedTypes);

    if (selectedClothes.isEmpty()) {
      return new RecommendationDto(weatherId, userId, List.of());
    }

    List<OotdDto> ootdList = recommendationMapper.toOotdDtoList(selectedClothes);

    log.info("추천 완료 weatherId={}, 추천 의상 수={}", weatherId, ootdList.size());

    return new RecommendationDto(weatherId, userId, ootdList);
  }

  double adjustTemperature(double currentTemp, int sensitivity) {
    double adjustment = (sensitivity - SENSITIVITY_CENTER) * SENSITIVITY_ADJUSTMENT_UNIT;
    return currentTemp + adjustment;
  }

  Set<ClothesType> getRecommendedTypes(double adjustedTemp) {
    Set<ClothesType> types = EnumSet.of(ClothesType.TOP, ClothesType.BOTTOM, ClothesType.SHOES);

    if (adjustedTemp <= VERY_COLD_MAX) {
      types.addAll(EnumSet.of(ClothesType.OUTER, ClothesType.SCARF, ClothesType.SOCKS));
    } else if (adjustedTemp <= COLD_MAX) {
      types.addAll(EnumSet.of(ClothesType.OUTER, ClothesType.SOCKS));
    } else if (adjustedTemp <= COOL_MAX) {
      types.add(ClothesType.OUTER);
    } else if (adjustedTemp > HOT_MAX) {
      types.add(ClothesType.HAT);
    }

    return types;
  }

  void applyPrecipitationAdjustment(Set<ClothesType> types,
      PrecipitationType precipitationType) {
    switch (precipitationType) {
      case RAIN, SHOWER -> {
        types.add(ClothesType.OUTER);
        types.add(ClothesType.HAT);
      }
      case SNOW, RAIN_SNOW -> {
        types.add(ClothesType.OUTER);
        types.add(ClothesType.SCARF);
        types.add(ClothesType.SOCKS);
      }
      case NONE -> { }
    }
  }

  void applyWindAdjustment(Set<ClothesType> types, WindStrength windStrength) {
    if (windStrength == WindStrength.STRONG) {
      types.add(ClothesType.OUTER);
    }
  }

  List<Clothes> selectClothesByType(List<Clothes> userClothes,
      Set<ClothesType> recommendedTypes) {
    List<Clothes> selected = new ArrayList<>();

    for (ClothesType type : recommendedTypes) {
      userClothes.stream()
          .filter(c -> c.getType() == type)
          .max(Comparator.comparing(Clothes::getCreatedAt))
          .ifPresent(selected::add);
    }

    return selected;
  }
}