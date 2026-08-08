package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesAttributeWithDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.ClothesAttribute;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesAttributeRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.ProfileNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDefValue;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository.ClothesAttributeDefValueRepository;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RecommendationService {

  // 기온대 경계값 (°C)
  private static final double VERY_COLD_MAX = 4.0;
  private static final double COLD_MAX = 8.0;
  private static final double COOL_MAX = 16.0;
  private static final double HOT_MAX = 27.0;

  // 민감도 보정 단위 (sensitivity 1단위당 1.5°C)
  private static final double SENSITIVITY_ADJUSTMENT_UNIT = 1.5;
  private static final int SENSITIVITY_CENTER = 3;

  private final WeatherRepository weatherRepository;
  private final ProfileRepository profileRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesAttributeRepository clothesAttributeRepository;
  private final ClothesAttributeDefValueRepository clothesAttributeDefValueRepository;

  public RecommendationDto recommend(UUID weatherId, UUID userId) {
    // 날씨 조회
    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> WeatherNotFoundException.withId(weatherId));

    // 프로필 조회
    Profile profile = profileRepository.findByIdWithUser(userId)
        .orElseThrow(ProfileNotFoundException::withNone);

    // 사용자 보유 의상 조회 (삭제 제외)
    List<Clothes> userClothes = clothesRepository
        .findAllByOwnerIdAndSoftDeletableDeletedAtIsNull(userId);

    if (userClothes.isEmpty()) {
      return new RecommendationDto(weatherId, userId, List.of());
    }

    // 체감온도 계산 (온도민감도 반영)
    double adjustedTemp = adjustTemperature(
        weather.getTemperatureCurrent(), profile.getTemperatureSensitivity());

    // 기온대별 추천 타입 결정
    Set<ClothesType> recommendedTypes = getRecommendedTypes(adjustedTemp);

    // 강수·바람 보정
    applyPrecipitationAdjustment(recommendedTypes, weather.getPrecipitationType());
    applyWindAdjustment(recommendedTypes, weather.getWindAsWord());

    // 타입별 1개씩 선택
    List<Clothes> selectedClothes = selectClothesByType(userClothes, recommendedTypes);

    if (selectedClothes.isEmpty()) {
      return new RecommendationDto(weatherId, userId, List.of());
    }

    // OotdDto 변환
    List<OotdDto> ootdList = toOotdDtoList(selectedClothes);

    log.info("추천 완료 weatherId={}, 추천 의상 수={}", weatherId, ootdList.size());

    return new RecommendationDto(weatherId, userId, ootdList);
  }

  // 체감온도 계산

  double adjustTemperature(double currentTemp, int sensitivity) {
    double adjustment = (sensitivity - SENSITIVITY_CENTER) * SENSITIVITY_ADJUSTMENT_UNIT;
    return currentTemp + adjustment;
  }

  //기온대별 추천 타입

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

  //강수 보정

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

  //바람 보정

  void applyWindAdjustment(Set<ClothesType> types, WindStrength windStrength) {
    if (windStrength == WindStrength.STRONG) {
      types.add(ClothesType.OUTER);
    }
  }

  //타입별 1개씩 선택

  List<Clothes> selectClothesByType(List<Clothes> userClothes,
      Set<ClothesType> recommendedTypes) {
    List<Clothes> selected = new ArrayList<>();

    for (ClothesType type : recommendedTypes) {
      userClothes.stream()
          .filter(c -> c.getType() == type)
          .findFirst()
          .ifPresent(selected::add);
    }

    return selected;
  }

  //OotdDto 변환

  private List<OotdDto> toOotdDtoList(List<Clothes> selectedClothes) {
    List<UUID> clothesIds = selectedClothes.stream()
        .map(Clothes::getId)
        .toList();

    // 속성 로딩
    List<ClothesAttribute> allAttributes =
        clothesAttributeRepository.findAllByClothesIdsWithDefinition(clothesIds);
    Map<UUID, List<ClothesAttribute>> attributesByClothesId = allAttributes.stream()
        .collect(Collectors.groupingBy(ClothesAttribute::getClothesId));

    // selectableValues 로딩
    List<UUID> definitionIds = allAttributes.stream()
        .map(attr -> attr.getDefinition().getId())
        .distinct()
        .toList();
    Map<UUID, List<ClothesAttributeDefValue>> defValuesByDefId = definitionIds.isEmpty()
        ? Map.of()
        : clothesAttributeDefValueRepository.findAllByDefinitionIds(definitionIds).stream()
            .collect(Collectors.groupingBy(v -> v.getDefinition().getId()));

    return selectedClothes.stream()
        .map(clothes -> {
          List<ClothesAttribute> attrs = attributesByClothesId
              .getOrDefault(clothes.getId(), List.of());

          List<ClothesAttributeWithDefDto> attrDtos = attrs.stream()
              .map(attr -> {
                List<String> selectableValues = defValuesByDefId
                    .getOrDefault(attr.getDefinition().getId(), List.of())
                    .stream()
                    .map(ClothesAttributeDefValue::getValue)
                    .toList();

                return new ClothesAttributeWithDefDto(
                    attr.getDefinition().getId(),
                    attr.getDefinition().getName(),
                    selectableValues,
                    attr.getValue());
              })
              .toList();

          return new OotdDto(
              clothes.getId(),
              clothes.getName(),
              clothes.getImageUrl(),
              clothes.getType(),
              attrDtos);
        })
        .toList();
  }
}