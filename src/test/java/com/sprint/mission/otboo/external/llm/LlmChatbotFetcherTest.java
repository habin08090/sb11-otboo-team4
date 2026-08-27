package com.sprint.mission.otboo.external.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Choice;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Message;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWeather;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmChatbotFetcherTest {

  private static final String MODEL = "test-model";

  static FixtureMonkey fixtureMonkey;

  @Mock
  LlmClient llmClient;

  LlmChatbotFetcher llmChatbotFetcher;

  @BeforeAll
  static void setUpFixtureMonkey() {
    fixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
        .plugin(new JakartaValidationPlugin())
        .build();
  }

  @BeforeEach
  void setUp() {
    llmChatbotFetcher = new LlmChatbotFetcher(llmClient, MODEL);
  }

  private static LlmChatbotContext context(List<LlmChatbotWardrobeItem> wardrobe) {
    return context(wardrobe, weather());
  }

  private static LlmChatbotContext context(List<LlmChatbotWardrobeItem> wardrobe,
      LlmChatbotWeather weather) {
    return fixtureMonkey.giveMeBuilder(LlmChatbotContext.class)
        .set("question", "오늘 뭐 입을까?")
        .set("weather", weather)
        .set("sensitivity", 3)
        .set("wardrobe", wardrobe)
        .sample();
  }

  private static LlmChatbotWeather weather() {
    return fixtureMonkey.giveMeBuilder(LlmChatbotWeather.class)
        .set("temperature", 28.0)
        .set("precipitationType", PrecipitationType.NONE)
        .set("windStrength", WindStrength.WEAK)
        .sample();
  }

  private static List<LlmChatbotWardrobeItem> wardrobe() {
    return List.of(fixtureMonkey.giveMeBuilder(LlmChatbotWardrobeItem.class)
        .set("name", "리넨 셔츠")
        .set("type", ClothesType.TOP)
        .set("attributeSummary", "얇음, 흰색")
        .sample());
  }

  private static LlmChatResponse responseOf(String content) {
    return new LlmChatResponse(List.of(new Choice(new Message("assistant", content))));
  }

  private static Request request() {
    return Request.create(Request.HttpMethod.POST, "/chat/completions", Map.of(),
        Request.Body.empty(), new RequestTemplate());
  }

  private static feign.Response response() {
    return feign.Response.builder()
        .request(request())
        .status(503)
        .reason("Service Unavailable")
        .headers(Map.of())
        .build();
  }

  @Nested
  @DisplayName("답변 요청")
  class Answer {

    @Test
    @DisplayName("컨텍스트를_프롬프트로_조립해_호출하고_응답_텍스트를_그대로_반환한다")
    void 컨텍스트를_프롬프트로_조립해_호출하고_응답_텍스트를_그대로_반환한다() {
      // given
      LlmChatbotContext context = context(wardrobe());
      given(llmClient.chat(any(LlmChatRequest.class)))
          .willReturn(responseOf("리넨 셔츠를 입으시면 좋겠어요."));

      // when
      String result = llmChatbotFetcher.answer(context);

      // then
      assertThat(result).isEqualTo("리넨 셔츠를 입으시면 좋겠어요.");

      ArgumentCaptor<LlmChatRequest> requestCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
      verify(llmClient).chat(requestCaptor.capture());
      LlmChatRequest sentRequest = requestCaptor.getValue();

      assertThat(sentRequest.model()).isEqualTo(MODEL);
      assertThat(sentRequest.messages()).hasSize(2);
      assertThat(sentRequest.messages().get(0).role()).isEqualTo("system");
      assertThat(sentRequest.messages().get(1).role()).isEqualTo("user");

      String userPrompt = sentRequest.messages().get(1).content();
      assertThat(userPrompt)
          .contains("오늘 뭐 입을까?")
          .contains("28.0")
          .contains(PrecipitationType.NONE.name())
          .contains(WindStrength.WEAK.name())
          .contains("3")
          .contains("리넨 셔츠")
          .contains(ClothesType.TOP.name())
          .contains("얇음, 흰색");
    }

    @Test
    @DisplayName("시스템_프롬프트에_옷차림_외_질문을_막는_가드가_포함된다")
    void 시스템_프롬프트에_옷차림_외_질문을_막는_가드가_포함된다() {
      // given
      given(llmClient.chat(any(LlmChatRequest.class))).willReturn(responseOf("답변"));

      // when
      llmChatbotFetcher.answer(context(wardrobe()));

      // then
      ArgumentCaptor<LlmChatRequest> requestCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
      verify(llmClient).chat(requestCaptor.capture());

      String systemPrompt = requestCaptor.getValue().messages().get(0).content();
      assertThat(systemPrompt).contains("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("옷장이_비어_있어도_호출에_성공한다")
    void 옷장이_비어_있어도_호출에_성공한다() {
      // given
      LlmChatbotContext context = context(List.of());
      given(llmClient.chat(any(LlmChatRequest.class))).willReturn(responseOf("답변"));

      // when
      String result = llmChatbotFetcher.answer(context);

      // then
      assertThat(result).isEqualTo("답변");
    }

    @Test
    @DisplayName("날씨_정보가_없으면_날씨_없이_프롬프트를_조립한다")
    void 날씨_정보가_없으면_날씨_없이_프롬프트를_조립한다() {
      // given
      LlmChatbotContext context = context(wardrobe(), null);
      given(llmClient.chat(any(LlmChatRequest.class))).willReturn(responseOf("답변"));

      // when
      String result = llmChatbotFetcher.answer(context);

      // then
      assertThat(result).isEqualTo("답변");

      ArgumentCaptor<LlmChatRequest> requestCaptor = ArgumentCaptor.forClass(LlmChatRequest.class);
      verify(llmClient).chat(requestCaptor.capture());

      String userPrompt = requestCaptor.getValue().messages().get(1).content();
      assertThat(userPrompt)
          .contains("오늘 뭐 입을까?")
          .contains("리넨 셔츠")
          .doesNotContain("기온:");
    }

    @Test
    @DisplayName("클라이언트가_FeignException을_던지면_LlmApiException으로_wrap한다")
    void 클라이언트가_FeignException을_던지면_LlmApiException으로_wrap한다() {
      // given
      LlmChatbotContext context = context(wardrobe());
      FeignException feignException = FeignException.errorStatus("LlmClient#chat", response());
      given(llmClient.chat(any(LlmChatRequest.class))).willThrow(feignException);

      // when & then
      assertThatThrownBy(() -> llmChatbotFetcher.answer(context))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("응답_본문이_공백이면_LlmApiException을_던진다")
    void 응답_본문이_공백이면_LlmApiException을_던진다() {
      // given
      LlmChatbotContext context = context(wardrobe());
      given(llmClient.chat(any(LlmChatRequest.class))).willReturn(responseOf("   "));

      // when & then
      assertThatThrownBy(() -> llmChatbotFetcher.answer(context))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("choices가_비어_있으면_LlmApiException을_던진다")
    void choices가_비어_있으면_LlmApiException을_던진다() {
      // given
      LlmChatbotContext context = context(wardrobe());
      given(llmClient.chat(any(LlmChatRequest.class))).willReturn(new LlmChatResponse(List.of()));

      // when & then
      assertThatThrownBy(() -> llmChatbotFetcher.answer(context))
          .isInstanceOf(LlmApiException.class);
    }
  }
}
