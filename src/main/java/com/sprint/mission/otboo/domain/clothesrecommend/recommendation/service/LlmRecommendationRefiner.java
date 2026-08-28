package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.cache.RecommendationCacheStore;
import com.sprint.mission.otboo.external.llm.LlmRecommendationFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class LlmRecommendationRefiner {

  private final LlmRecommendationFetcher llmRecommendationFetcher;
  private final RecommendationCacheStore recommendationCacheStore;

  /**
   * LLM이 추린 추천 후보군을 돌려준다.
   *
   * <p>완성된 조합이 아니라 후보군을 돌려주는 이유는, 최종 조합을 매 요청마다 다르게 구성하기 위해서다. 조합을 캐싱하면 옷장이 그대로인 한 항상 같은 답이 나와
   * "다른 옷 추천"이 동작하지 않는다. 조합 구성은 {@link OutfitComposer}가 맡는다.
   *
   * <p>LLM 호출이나 검증에 실패하면 후보 전체를 그대로 돌려준다. 추천 자체를 실패시키는 것보다 낫다.
   */
  public List<Clothes> selectPool(LlmRecommendationContext context, List<Clothes> candidates) {
    Map<UUID, Clothes> candidatesById = candidates.stream()
        .collect(Collectors.toMap(Clothes::getId, Function.identity()));

    Optional<List<Clothes>> cached = findCached(context, candidatesById);
    if (cached.isPresent()) {
      return cached.orElseThrow();
    }

    LlmSelectedClothes selected;
    try {
      selected = llmRecommendationFetcher.select(context);
    } catch (LlmException e) {
      log.error("LLM 추천 후보군 선정 실패, 후보 전체로 대체", e);
      return candidates;
    }

    Optional<List<Clothes>> resolved = resolve(selected.clothesIds(), candidatesById);
    if (resolved.isEmpty()) {
      log.warn("LLM 후보군이 검증을 통과하지 못함, 후보 전체로 대체");
      return candidates;
    }

    recommendationCacheStore.save(context, selected.clothesIds());
    return resolved.orElseThrow();
  }

  /**
   * 캐시에 남은 선택도 지금의 후보 목록으로 다시 검증한다. 캐시 키가 후보 전체에서 파생되므로 이론상 항상 유효하지만, 어긋난 값이 그대로 응답에 실리는 것보다 LLM을
   * 한 번 더 호출하는 편이 안전하다.
   */
  private Optional<List<Clothes>> findCached(LlmRecommendationContext context,
      Map<UUID, Clothes> candidatesById) {
    Optional<List<UUID>> cachedIds = recommendationCacheStore.find(context);
    if (cachedIds.isEmpty()) {
      return Optional.empty();
    }

    Optional<List<Clothes>> cached = resolve(cachedIds.orElseThrow(), candidatesById);
    if (cached.isEmpty()) {
      log.warn("캐시된 선택이 현재 후보와 맞지 않음, LLM을 다시 호출한다");
      return Optional.empty();
    }

    log.debug("LLM 추천 캐시 적중");
    return cached;
  }

  /**
   * 후보군은 종류당 여러 벌을 담으므로 종류 중복을 막지 않는다. 종류 중복 없음과 원피스·상하의 배타는 최종 조합의 규칙이라 {@link OutfitComposer}가
   * 보장한다. 여기서는 LLM이 후보에 없는 의상을 지어내지 않았는지만 확인한다.
   */
  private Optional<List<Clothes>> resolve(List<UUID> clothesIds,
      Map<UUID, Clothes> candidatesById) {
    // 빈 후보군은 LLM이 아무것도 고르지 않은 것과 같아 후보군으로 쓸 수 없다. 파서가 빈 응답을 이미
    // 막고 있지만, 그 보장에 기대지 않고 여기서도 확인한다.
    if (clothesIds.isEmpty()) {
      return Optional.empty();
    }

    List<Clothes> resolved = new ArrayList<>();
    for (UUID clothesId : clothesIds) {
      Clothes clothes = candidatesById.get(clothesId);
      if (clothes == null) {
        log.warn("선택된 의상이 후보에 없음 clothesId={}", clothesId);
        return Optional.empty();
      }
      resolved.add(clothes);
    }
    return Optional.of(resolved);
  }
}
