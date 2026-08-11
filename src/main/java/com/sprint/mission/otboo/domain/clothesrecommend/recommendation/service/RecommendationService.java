package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.ProfileNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.mapper.RecommendationOotdAssembler;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
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
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;

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
  private final RecommendationOotdAssembler recommendationOotdAssembler;

  public RecommendationDto recommend(UUID weatherId, UUID userId) {
    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> WeatherNotFoundException.withId(weatherId));

    Profile profile = profileRepository.findByIdWithUser(userId)
        .orElseThrow(() -> ProfileNotFoundException.withUserId(userId));

    double adjustedTemp = adjustTemperature(
        weather.getTemperatureCurrent(), profile.getTemperatureSensitivity());

    Set<ClothesType> recommendedTypes = getRecommendedTypes(adjustedTemp);

    applyPrecipitationAdjustment(recommendedTypes, weather.getPrecipitationType());
    applyWindAdjustment(recommendedTypes, weather.getWindAsWord());

    List<Clothes> userClothes = clothesRepository
        .findActiveByOwnerIdAndTypeIn(userId, recommendedTypes);

    if (userClothes.isEmpty()) {
      log.info("추천 가능한 의상 없음 weatherId={}, userId={}", weatherId, userId);
      return new RecommendationDto(weatherId, userId, List.of());
    }

    List<Clothes> selectedClothes = selectClothesByType(userClothes, recommendedTypes);

    List<OotdDto> ootdList = recommendationOotdAssembler.toOotdDtoList(selectedClothes);

    log.info("추천 완료 weatherId={}, 추천 의상 수={}", weatherId, ootdList.size());

    return new RecommendationDto(weatherId, userId, ootdList);
  }

  double adjustTemperature(double currentTemp, int sensitivity) {
    double adjustment = (sensitivity - SENSITIVITY_CENTER) * SENSITIVITY_ADJUSTMENT_UNIT;
    return currentTemp + adjustment;
  }

  Set<ClothesType> getRecommendedTypes(double adjustedTemp) {
    Set<ClothesType> types = EnumSet.noneOf(ClothesType.class);
    types.add(ClothesType.SHOES);

    if (adjustedTemp <= VERY_COLD_MAX) {
      types.addAll(EnumSet.of(
          ClothesType.TOP, ClothesType.BOTTOM,
          ClothesType.OUTER, ClothesType.SCARF, ClothesType.SOCKS));
    } else if (adjustedTemp <= COLD_MAX) {
      types.addAll(EnumSet.of(
          ClothesType.TOP, ClothesType.BOTTOM,
          ClothesType.OUTER, ClothesType.SOCKS));
    } else if (adjustedTemp <= COOL_MAX) {
      types.addAll(EnumSet.of(
          ClothesType.TOP, ClothesType.BOTTOM,
          ClothesType.OUTER));
    } else {
      // 따뜻한 날씨: DRESS(원피스) 또는 TOP+BOTTOM 중 택1 (배타)
      types.add(ClothesType.DRESS);
      types.add(ClothesType.TOP);
      types.add(ClothesType.BOTTOM);
      if (adjustedTemp > HOT_MAX) {
        types.add(ClothesType.HAT);
      }
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
    boolean dressSelected = false;

    // DRESS가 후보에 있으면 먼저 확인 — DRESS가 있으면 TOP/BOTTOM 대체
    if (recommendedTypes.contains(ClothesType.DRESS)) {
      userClothes.stream()
          .filter(c -> c.getType() == ClothesType.DRESS)
          .max(Comparator.comparing(Clothes::getCreatedAt)
              .thenComparing(Clothes::getId))
          .ifPresent(dress -> {
            selected.add(dress);
          });
      dressSelected = !selected.isEmpty();
    }

    for (ClothesType type : recommendedTypes) {
      // DRESS는 이미 처리됨
      if (type == ClothesType.DRESS) {
        continue;
      }
      // DRESS가 선택됐으면 TOP/BOTTOM은 건너뜀
      if (dressSelected && (type == ClothesType.TOP || type == ClothesType.BOTTOM)) {
        continue;
      }

      userClothes.stream()
          .filter(c -> c.getType() == type)
          .max(Comparator.comparing(Clothes::getCreatedAt)
              .thenComparing(Clothes::getId))
          .ifPresent(selected::add);
    }

    return selected;
  }
}