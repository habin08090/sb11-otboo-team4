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
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationCandidate;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final RecommendationOotdAssembler recommendationOotdAssembler;
  private final LlmRecommendationRefiner llmRecommendationRefiner;
  private final OutfitComposer outfitComposer;

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

    LlmRecommendationContext llmContext = buildLlmContext(
        weather.getTemperatureCurrent(), adjustedTemp,
        weather.getPrecipitationType(), weather.getWindAsWord(),
        profile.getTemperatureSensitivity(), userClothes);
    List<Clothes> pool = coverMissingTypes(
        llmRecommendationRefiner.selectPool(llmContext, userClothes), userClothes);

    List<Clothes> selectedClothes = outfitComposer.compose(pool, recommendedTypes);

    List<OotdDto> ootdList = recommendationOotdAssembler.toOotdDtoList(selectedClothes);

    log.info("추천 완료 weatherId={}, 추천 의상 수={}", weatherId, ootdList.size());

    return new RecommendationDto(weatherId, userId, ootdList);
  }

  /**
   * LLM이 통째로 빠뜨린 종류를 보유 의상으로 메운다.
   *
   * <p>LLM은 후보군을 고를 때 신발이나 가방처럼 특정 종류를 아예 언급하지 않을 때가 있다. 그대로 두면 신발 없는 코디가 추천된다. 프롬프트로 부탁하는 것만으로는
   * 보장되지 않아 코드로 메운다.
   *
   * <p>LLM이 한 벌이라도 고른 종류는 건드리지 않는다. 그 종류에 대해서는 LLM의 판단을 존중한다.
   */
  List<Clothes> coverMissingTypes(List<Clothes> pool, List<Clothes> userClothes) {
    Set<ClothesType> coveredTypes = pool.stream()
        .map(Clothes::getType)
        .collect(Collectors.toSet());

    List<Clothes> missing = userClothes.stream()
        .filter(clothes -> !coveredTypes.contains(clothes.getType()))
        .toList();

    if (missing.isEmpty()) {
      return pool;
    }

    // 보정된 종류는 LLM의 날씨 판단을 거치지 않은 채 후보에 들어간다. 안전망이지 정상 경로가 아니므로,
    // 자주 찍히면 프롬프트를 손봐야 한다는 신호다.
    Set<ClothesType> missingTypes = missing.stream()
        .map(Clothes::getType)
        .collect(Collectors.toSet());
    log.warn("LLM이 빠뜨린 종류를 보유 의상으로 보정한다 types={}, 보정수={}",
        missingTypes, missing.size());
    return Stream.concat(pool.stream(), missing.stream()).toList();
  }

  LlmRecommendationContext buildLlmContext(double currentTemp, double adjustedTemp,
      PrecipitationType precipitationType, WindStrength windStrength, int sensitivity,
      List<Clothes> candidates) {
    List<LlmRecommendationCandidate> llmCandidates = candidates.stream()
        .map(c -> new LlmRecommendationCandidate(c.getId(), c.getName(), c.getType(), ""))
        .toList();
    return new LlmRecommendationContext(
        currentTemp, adjustedTemp, precipitationType, windStrength, sensitivity, llmCandidates);
  }

  double adjustTemperature(double currentTemp, int sensitivity) {
    double adjustment = (sensitivity - SENSITIVITY_CENTER) * SENSITIVITY_ADJUSTMENT_UNIT;
    return currentTemp + adjustment;
  }

  Set<ClothesType> getRecommendedTypes(double adjustedTemp) {
    Set<ClothesType> types = EnumSet.noneOf(ClothesType.class);
    types.add(ClothesType.SHOES);
    // 날씨와 무관하지만 코디를 이루는 품목이라 항상 후보에 넣는다.
    // UNDERWEAR·ETC는 OOTD에 어울리지 않거나 성격이 섞여 있어 제외한다.
    types.add(ClothesType.BAG);
    types.add(ClothesType.ACCESSORY);

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

}