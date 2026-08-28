package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.cache.RecommendationCacheStore;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.external.llm.LlmRecommendationFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmRecommendationRefinerTest {

  @Mock
  private LlmRecommendationFetcher llmRecommendationFetcher;

  @Mock
  private RecommendationCacheStore recommendationCacheStore;

  private final LlmRecommendationContext context = new LlmRecommendationContext(
      5.0, 3.5, PrecipitationType.RAIN, WindStrength.STRONG, 3, List.of());

  private LlmRecommendationRefiner refiner() {
    return new LlmRecommendationRefiner(llmRecommendationFetcher, recommendationCacheStore);
  }

  @Nested
  @DisplayName("후보군 선정")
  class SelectPool {

    @Test
    @DisplayName("LLM이_후보_내에서_고르면_그_후보군을_반환한다")
    void LLM이_후보_내에서_고르면_그_후보군을_반환한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      Clothes bottom = Clothes.create(UUID.randomUUID(), "청바지", ClothesType.BOTTOM);
      List<Clothes> candidates = List.of(top, bottom);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top.getId(), bottom.getId())));

      List<Clothes> result = refiner().selectPool(context, candidates);

      assertThat(result).containsExactly(top, bottom);
    }

    @Test
    @DisplayName("같은_종류를_여러_벌_고르면_그대로_후보군에_담는다")
    void 같은_종류를_여러_벌_고르면_그대로_후보군에_담는다() {
      // 후보군은 종류당 여러 벌을 담는 것이 정상이다. 한 벌만 남기는 일은 조합 단계에서 한다.
      Clothes top1 = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      Clothes top2 = Clothes.create(UUID.randomUUID(), "셔츠", ClothesType.TOP);
      List<Clothes> candidates = List.of(top1, top2);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top1.getId(), top2.getId())));

      List<Clothes> result = refiner().selectPool(context, candidates);

      assertThat(result).containsExactly(top1, top2);
    }

    @Test
    @DisplayName("DRESS와_TOP이_함께_있어도_후보군에_담는다")
    void DRESS와_TOP이_함께_있어도_후보군에_담는다() {
      // 원피스와 상하의의 배타는 최종 조합의 규칙이라 후보군 단계에서는 막지 않는다.
      Clothes dress = Clothes.create(UUID.randomUUID(), "원피스", ClothesType.DRESS);
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(dress, top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(dress.getId(), top.getId())));

      List<Clothes> result = refiner().selectPool(context, candidates);

      assertThat(result).containsExactly(dress, top);
    }

    @Test
    @DisplayName("후보에_없는_id가_포함되면_후보_전체로_폴백한다")
    void 후보에_없는_id가_포함되면_후보_전체로_폴백한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(UUID.randomUUID())));

      List<Clothes> result = refiner().selectPool(context, candidates);

      assertThat(result).isEqualTo(candidates);
    }

    @Test
    @DisplayName("LLM_호출이_실패하면_후보_전체로_폴백한다")
    void LLM_호출이_실패하면_후보_전체로_폴백한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willThrow(LlmApiException.callFailed(new RuntimeException("connection reset")));

      List<Clothes> result = refiner().selectPool(context, candidates);

      assertThat(result).isEqualTo(candidates);
    }

    @Test
    @DisplayName("LLM_응답_파싱이_실패하면_후보_전체로_폴백한다")
    void LLM_응답_파싱이_실패하면_후보_전체로_폴백한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willThrow(LlmApiException.parseFailed());

      List<Clothes> result = refiner().selectPool(context, candidates);

      assertThat(result).isEqualTo(candidates);
    }
  }

  @Nested
  @DisplayName("캐시 조회")
  class CacheHit {

    @Test
    @DisplayName("캐시에_값이_있으면_LLM을_호출하지_않고_캐시_후보군을_반환한다")
    void 캐시에_값이_있으면_LLM을_호출하지_않고_캐시_후보군을_반환한다() {
      // given
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      Clothes bottom = Clothes.create(UUID.randomUUID(), "청바지", ClothesType.BOTTOM);
      List<Clothes> candidates = List.of(top, bottom);

      given(recommendationCacheStore.find(context))
          .willReturn(Optional.of(List.of(top.getId(), bottom.getId())));

      // when
      List<Clothes> result = refiner().selectPool(context, candidates);

      // then
      assertThat(result).containsExactly(top, bottom);
      verify(llmRecommendationFetcher, never()).select(any(LlmRecommendationContext.class));
      verify(recommendationCacheStore, never()).save(any(LlmRecommendationContext.class), anyList());
    }

    @Test
    @DisplayName("캐시에_빈_목록이_들어_있으면_적중으로_보지_않고_LLM을_다시_호출한다")
    void 캐시에_빈_목록이_들어_있으면_적중으로_보지_않고_LLM을_다시_호출한다() {
      // given - 빈 후보군을 그대로 쓰면 LLM이 아무것도 고르지 않은 것과 같아진다
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(recommendationCacheStore.find(context)).willReturn(Optional.of(List.of()));
      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top.getId())));

      // when
      List<Clothes> result = refiner().selectPool(context, candidates);

      // then
      assertThat(result).containsExactly(top);
      verify(llmRecommendationFetcher).select(context);
    }

    @Test
    @DisplayName("캐시된_id가_현재_후보에_없으면_LLM을_다시_호출한다")
    void 캐시된_id가_현재_후보에_없으면_LLM을_다시_호출한다() {
      // given
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(recommendationCacheStore.find(context))
          .willReturn(Optional.of(List.of(UUID.randomUUID())));
      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top.getId())));

      // when
      List<Clothes> result = refiner().selectPool(context, candidates);

      // then
      assertThat(result).containsExactly(top);
      verify(llmRecommendationFetcher).select(context);
    }
  }

  @Nested
  @DisplayName("캐시 저장")
  class CacheSave {

    @Test
    @DisplayName("캐시가_비면_LLM_후보군을_캐시에_저장한다")
    void 캐시가_비면_LLM_후보군을_캐시에_저장한다() {
      // given
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      Clothes bottom = Clothes.create(UUID.randomUUID(), "청바지", ClothesType.BOTTOM);
      List<Clothes> candidates = List.of(top, bottom);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top.getId(), bottom.getId())));

      // when
      refiner().selectPool(context, candidates);

      // then
      verify(recommendationCacheStore).save(context, List.of(top.getId(), bottom.getId()));
    }

    @Test
    @DisplayName("LLM_호출이_실패해_폴백하면_캐시에_저장하지_않는다")
    void LLM_호출이_실패해_폴백하면_캐시에_저장하지_않는다() {
      // given
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willThrow(LlmApiException.callFailed(new RuntimeException("connection reset")));

      // when
      refiner().selectPool(context, candidates);

      // then
      verify(recommendationCacheStore, never()).save(any(LlmRecommendationContext.class), anyList());
    }

    @Test
    @DisplayName("후보에_없는_id가_섞여_폴백하면_캐시에_저장하지_않는다")
    void 후보에_없는_id가_섞여_폴백하면_캐시에_저장하지_않는다() {
      // given
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top.getId(), UUID.randomUUID())));

      // when
      refiner().selectPool(context, candidates);

      // then
      verify(recommendationCacheStore, never()).save(any(LlmRecommendationContext.class), anyList());
    }
  }
}
