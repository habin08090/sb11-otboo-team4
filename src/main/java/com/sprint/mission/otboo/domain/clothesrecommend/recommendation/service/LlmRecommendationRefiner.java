package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.external.llm.LlmRecommendationFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  public List<Clothes> refine(LlmRecommendationContext context, List<Clothes> candidates,
      List<Clothes> fallback) {
    LlmSelectedClothes selected;
    try {
      selected = llmRecommendationFetcher.select(context);
    } catch (LlmException e) {
      log.error("LLM 추천 재선정 실패, 규칙 기반 결과로 대체", e);
      return fallback;
    }

    Map<UUID, Clothes> candidatesById = candidates.stream()
        .collect(Collectors.toMap(Clothes::getId, Function.identity()));

    List<Clothes> resolved = new ArrayList<>();
    for (UUID clothesId : selected.clothesIds()) {
      Clothes clothes = candidatesById.get(clothesId);
      if (clothes == null) {
        log.warn("LLM이 후보에 없는 의상을 선택함, 규칙 기반 결과로 대체 clothesId={}", clothesId);
        return fallback;
      }
      resolved.add(clothes);
    }

    if (hasDuplicateType(resolved)) {
      log.warn("LLM 선택에 동일 타입이 중복됨, 규칙 기반 결과로 대체");
      return fallback;
    }

    if (hasDressWithTopOrBottom(resolved)) {
      log.warn("LLM 선택에 DRESS와 TOP/BOTTOM이 동시에 포함됨, 규칙 기반 결과로 대체");
      return fallback;
    }

    return resolved;
  }

  private boolean hasDuplicateType(List<Clothes> clothesList) {
    Set<ClothesType> types = clothesList.stream().map(Clothes::getType).collect(Collectors.toSet());
    return types.size() != clothesList.size();
  }

  private boolean hasDressWithTopOrBottom(List<Clothes> clothesList) {
    Set<ClothesType> types = clothesList.stream().map(Clothes::getType).collect(Collectors.toSet());
    boolean hasDress = types.contains(ClothesType.DRESS);
    boolean hasTopOrBottom = types.contains(ClothesType.TOP) || types.contains(ClothesType.BOTTOM);
    return hasDress && hasTopOrBottom;
  }
}
