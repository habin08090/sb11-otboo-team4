package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.external.llm.LlmRecommendationFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import java.util.List;
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

  private final LlmRecommendationContext context = new LlmRecommendationContext(
      5.0, 3.5, PrecipitationType.RAIN, WindStrength.STRONG, 3, List.of());

  private LlmRecommendationRefiner refiner() {
    return new LlmRecommendationRefiner(llmRecommendationFetcher);
  }

  @Nested
  @DisplayName("재선정 검증")
  class Refine {

    @Test
    @DisplayName("LLM이_후보_내에서_유효하게_선택하면_그_결과를_반환한다")
    void LLM이_후보_내에서_유효하게_선택하면_그_결과를_반환한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      Clothes bottom = Clothes.create(UUID.randomUUID(), "청바지", ClothesType.BOTTOM);
      List<Clothes> candidates = List.of(top, bottom);
      List<Clothes> fallback = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top.getId(), bottom.getId())));

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).containsExactly(top, bottom);
    }

    @Test
    @DisplayName("후보에_없는_id가_포함되면_규칙_기반_결과로_폴백한다")
    void 후보에_없는_id가_포함되면_규칙_기반_결과로_폴백한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);
      List<Clothes> fallback = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(UUID.randomUUID())));

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).isEqualTo(fallback);
    }

    @Test
    @DisplayName("동일_타입이_중복되면_규칙_기반_결과로_폴백한다")
    void 동일_타입이_중복되면_규칙_기반_결과로_폴백한다() {
      Clothes top1 = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      Clothes top2 = Clothes.create(UUID.randomUUID(), "셔츠", ClothesType.TOP);
      List<Clothes> candidates = List.of(top1, top2);
      List<Clothes> fallback = List.of(top1);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(top1.getId(), top2.getId())));

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).isEqualTo(fallback);
    }

    @Test
    @DisplayName("DRESS와_TOP이_동시에_포함되면_규칙_기반_결과로_폴백한다")
    void DRESS와_TOP이_동시에_포함되면_규칙_기반_결과로_폴백한다() {
      Clothes dress = Clothes.create(UUID.randomUUID(), "원피스", ClothesType.DRESS);
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(dress, top);
      List<Clothes> fallback = List.of(dress);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(dress.getId(), top.getId())));

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).isEqualTo(fallback);
    }

    @Test
    @DisplayName("DRESS와_BOTTOM이_동시에_포함되면_규칙_기반_결과로_폴백한다")
    void DRESS와_BOTTOM이_동시에_포함되면_규칙_기반_결과로_폴백한다() {
      Clothes dress = Clothes.create(UUID.randomUUID(), "원피스", ClothesType.DRESS);
      Clothes bottom = Clothes.create(UUID.randomUUID(), "청바지", ClothesType.BOTTOM);
      List<Clothes> candidates = List.of(dress, bottom);
      List<Clothes> fallback = List.of(dress);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willReturn(new LlmSelectedClothes(List.of(dress.getId(), bottom.getId())));

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).isEqualTo(fallback);
    }

    @Test
    @DisplayName("LLM_호출이_실패하면_규칙_기반_결과로_폴백한다")
    void LLM_호출이_실패하면_규칙_기반_결과로_폴백한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);
      List<Clothes> fallback = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willThrow(LlmApiException.callFailed(new RuntimeException("connection reset")));

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).isEqualTo(fallback);
    }

    @Test
    @DisplayName("LLM_응답_파싱이_실패하면_규칙_기반_결과로_폴백한다")
    void LLM_응답_파싱이_실패하면_규칙_기반_결과로_폴백한다() {
      Clothes top = Clothes.create(UUID.randomUUID(), "니트", ClothesType.TOP);
      List<Clothes> candidates = List.of(top);
      List<Clothes> fallback = List.of(top);

      given(llmRecommendationFetcher.select(any(LlmRecommendationContext.class)))
          .willThrow(LlmApiException.parseFailed());

      List<Clothes> result = refiner().refine(context, candidates, fallback);

      assertThat(result).isEqualTo(fallback);
    }
  }
}
