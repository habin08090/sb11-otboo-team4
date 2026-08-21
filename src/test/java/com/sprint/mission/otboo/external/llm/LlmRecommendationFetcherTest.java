package com.sprint.mission.otboo.external.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Choice;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Message;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationCandidate;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmRecommendationFetcherTest {

  @Mock
  private LlmClient llmClient;
  @Mock
  private LlmRecommendationParser llmRecommendationParser;

  private static LlmRecommendationContext context() {
    UUID candidateId = UUID.randomUUID();
    return new LlmRecommendationContext(
        5.0, 3.5, PrecipitationType.RAIN, WindStrength.STRONG, 3,
        List.of(new LlmRecommendationCandidate(candidateId, "패딩", ClothesType.OUTER, "두꺼움"))
    );
  }

  @Nested
  @DisplayName("재선정 요청")
  class Select {

    @Test
    @DisplayName("컨텍스트를_프롬프트로_조립해_호출하고_파싱_결과를_반환한다")
    void 컨텍스트를_프롬프트로_조립해_호출하고_파싱_결과를_반환한다() {
      LlmRecommendationFetcher fetcher = new LlmRecommendationFetcher(
          llmClient, llmRecommendationParser, "test-model");
      LlmRecommendationContext context = context();
      LlmChatResponse response =
          new LlmChatResponse(List.of(new Choice(new Message("assistant", "{}"))));
      LlmSelectedClothes selected = new LlmSelectedClothes(
          List.of(context.candidates().get(0).clothesId()));

      given(llmClient.chat(any(LlmChatRequest.class))).willReturn(response);
      given(llmRecommendationParser.parse(response)).willReturn(selected);

      LlmSelectedClothes result = fetcher.select(context);

      assertThat(result).isEqualTo(selected);

      ArgumentCaptor<LlmChatRequest> requestCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
      verify(llmClient).chat(requestCaptor.capture());
      LlmChatRequest sentRequest = requestCaptor.getValue();

      assertThat(sentRequest.model()).isEqualTo("test-model");
      assertThat(sentRequest.messages()).hasSize(2);
      assertThat(sentRequest.messages().get(0).role()).isEqualTo("system");
      assertThat(sentRequest.messages().get(1).role()).isEqualTo("user");

      String userPrompt = sentRequest.messages().get(1).content();
      assertThat(userPrompt)
          .contains("5.0")
          .contains("3.5")
          .contains(PrecipitationType.RAIN.name())
          .contains(WindStrength.STRONG.name())
          .contains("3")
          .contains(context.candidates().get(0).clothesId().toString())
          .contains("패딩")
          .contains(ClothesType.OUTER.name());
    }

    @Test
    @DisplayName("클라이언트가_FeignException을_던지면_LlmApiException으로_wrap한다")
    void 클라이언트가_FeignException을_던지면_LlmApiException으로_wrap한다() {
      LlmRecommendationFetcher fetcher = new LlmRecommendationFetcher(
          llmClient, llmRecommendationParser, "test-model");
      LlmRecommendationContext context = context();

      FeignException feignException =
          FeignException.errorStatus("LlmClient#chat", response());
      given(llmClient.chat(any(LlmChatRequest.class))).willThrow(feignException);

      assertThatThrownBy(() -> fetcher.select(context))
          .isInstanceOf(LlmApiException.class);
      verifyNoInteractions(llmRecommendationParser);
    }

    private Request request() {
      return Request.create(Request.HttpMethod.POST, "/chat/completions", Map.of(),
          Request.Body.empty(), new RequestTemplate());
    }

    private feign.Response response() {
      return feign.Response.builder()
          .request(request())
          .status(503)
          .reason("Service Unavailable")
          .headers(Map.of())
          .build();
    }
  }
}
