package com.sprint.mission.otboo.domain.clothesrecommend.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception.ClothesExtractionBadRequestException;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception.ClothesExtractionFailedException;
import com.sprint.mission.otboo.external.llm.LlmClient;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionResponse.Choice;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionResponse.Message;
import com.sprint.mission.otboo.external.purchase.PurchasePageClient;
import com.sprint.mission.otboo.external.purchase.PurchasePageParser;
import com.sprint.mission.otboo.external.purchase.dto.PurchasePageResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sprint.mission.otboo.external.llm.LlmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class ClothesExtractionServiceTest {

  private static final FixtureMonkey fixtureMonkey = FixtureMonkey.builder()
      .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
      .build();

  @InjectMocks
  ClothesExtractionService clothesExtractionService;

  @Mock
  PurchasePageClient purchasePageClient;

  @Mock
  PurchasePageParser purchasePageParser;

  @Mock
  LlmClient llmClient;

  @Mock
  LlmProperties llmProperties;

  @Spy
  ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  @DisplayName("OG 태그 파싱 성공")
  class OgTagSuccess {

    @Test
    @DisplayName("OG 태그가 있으면 ClothesDto를 반환한다")
    void OG_태그가_있으면_ClothesDto를_반환한다() {
      // given
      String url = "https://www.musinsa.com/products/12345";
      String html = "<html><head><meta property='og:title' content='데님 자켓'/></head></html>";

      PurchasePageResponse ogResult = fixtureMonkey.giveMeBuilder(PurchasePageResponse.class)
          .set("title", "데님 자켓")
          .set("imageUrl", "https://image.musinsa.com/goods/001.jpg")
          .sample();

      when(purchasePageClient.fetchPage(any())).thenReturn(html);
      when(purchasePageParser.parse(html)).thenReturn(ogResult);

      // when
      ClothesDto result = clothesExtractionService.extractByUrl(url);

      // then
      assertThat(result.id()).isNull();
      assertThat(result.ownerId()).isNull();
      assertThat(result.name()).isEqualTo("데님 자켓");
      assertThat(result.imageUrl()).isEqualTo("https://image.musinsa.com/goods/001.jpg");
    }
  }

  @Nested
  @DisplayName("LLM 폴백")
  class LlmFallback {

    @Test
    @DisplayName("OG 태그 실패 시 LLM으로 추출한다")
    void OG_태그_실패시_LLM으로_추출한다() {
      // given
      String url = "https://www.musinsa.com/products/12345";
      String html = "<html><body><h1>데님 자켓</h1></body></html>";

      PurchasePageResponse emptyOg = new PurchasePageResponse(null, null, null, null);

      String llmJson = """
          {"name": "데님 자켓", "imageUrl": null}
          """;
      LlmExtractionResponse llmResponse = new LlmExtractionResponse(
          List.of(new Choice(new Message("assistant", llmJson)))
      );

      when(llmProperties.apiKey()).thenReturn("test-key");
      when(llmProperties.model()).thenReturn("test-model");
      when(purchasePageClient.fetchPage(any())).thenReturn(html);
      when(purchasePageParser.parse(html)).thenReturn(emptyOg);
      when(llmClient.extract(anyString(), any())).thenReturn(llmResponse);

      // when
      ClothesDto result = clothesExtractionService.extractByUrl(url);

      // then
      assertThat(result.name()).isEqualTo("데님 자켓");
    }
  }

  @Nested
  @DisplayName("예외 케이스")
  class ExceptionCases {

    @Test
    @DisplayName("URL이 빈 문자열이면 예외가 발생한다")
    void URL이_빈_문자열이면_예외가_발생한다() {
      // when & then
      assertThatThrownBy(() -> clothesExtractionService.extractByUrl(""))
          .isInstanceOf(ClothesExtractionBadRequestException.class);
    }

    @Test
    @DisplayName("URL이 null이면 예외가 발생한다")
    void URL이_null이면_예외가_발생한다() {
      // when & then
      assertThatThrownBy(() -> clothesExtractionService.extractByUrl(null))
          .isInstanceOf(ClothesExtractionBadRequestException.class);
    }

    @Test
    @DisplayName("HTTP URL이면 예외가 발생한다")
    void HTTP_URL이면_예외가_발생한다() {
      // when & then
      assertThatThrownBy(() ->
          clothesExtractionService.extractByUrl("http://www.musinsa.com/products/12345"))
          .isInstanceOf(ClothesExtractionBadRequestException.class);
    }

    @Test
    @DisplayName("허용되지 않은 호스트이면 예외가 발생한다")
    void 허용되지_않은_호스트이면_예외가_발생한다() {
      // when & then
      assertThatThrownBy(() ->
          clothesExtractionService.extractByUrl("https://evil.com/products/12345"))
          .isInstanceOf(ClothesExtractionBadRequestException.class);
    }
  }

  @Nested
  @DisplayName("Feign 호출 실패")
  class FeignFailure {

    @Test
    @DisplayName("페이지 가져오기 실패 시 예외가 전파된다")
    void 페이지_가져오기_실패시_예외가_전파된다() {
      // given
      String url = "https://www.musinsa.com/products/12345";
      when(purchasePageClient.fetchPage(any()))
          .thenThrow(new RuntimeException("Connection refused"));

      // when & then
      assertThatThrownBy(() -> clothesExtractionService.extractByUrl(url))
          .isInstanceOf(ClothesExtractionFailedException.class);
    }
  }

  @Nested
  @DisplayName("LLM 응답 파싱 실패")
  class LlmParseFailure {

    @Test
    @DisplayName("LLM이 잘못된 JSON을 반환하면 예외가 발생한다")
    void LLM이_잘못된_JSON을_반환하면_예외가_발생한다() {
      // given
      String url = "https://www.musinsa.com/products/12345";
      String html = "<html><body>상품</body></html>";

      PurchasePageResponse emptyOg = new PurchasePageResponse(null, null, null, null);

      LlmExtractionResponse badResponse = new LlmExtractionResponse(
          List.of(new Choice(new Message("assistant", "이건 JSON이 아닙니다")))
      );

      when(llmProperties.apiKey()).thenReturn("test-key");
      when(llmProperties.model()).thenReturn("test-model");
      when(purchasePageClient.fetchPage(any())).thenReturn(html);
      when(purchasePageParser.parse(html)).thenReturn(emptyOg);
      when(llmClient.extract(anyString(), any())).thenReturn(badResponse);

      // when & then
      assertThatThrownBy(() -> clothesExtractionService.extractByUrl(url))
          .isInstanceOf(ClothesExtractionFailedException.class);
    }

    @Test
    @DisplayName("LLM 응답이 빈 content이면 예외가 발생한다")
    void LLM_응답이_빈_content이면_예외가_발생한다() {
      // given
      String url = "https://www.musinsa.com/products/12345";
      String html = "<html><body>상품</body></html>";

      PurchasePageResponse emptyOg = new PurchasePageResponse(null, null, null, null);

      LlmExtractionResponse emptyResponse = new LlmExtractionResponse(
          List.of(new Choice(new Message("assistant", "")))
      );

      when(llmProperties.apiKey()).thenReturn("test-key");
      when(llmProperties.model()).thenReturn("test-model");
      when(purchasePageClient.fetchPage(any())).thenReturn(html);
      when(purchasePageParser.parse(html)).thenReturn(emptyOg);
      when(llmClient.extract(anyString(), any())).thenReturn(emptyResponse);

      // when & then
      assertThatThrownBy(() -> clothesExtractionService.extractByUrl(url))
          .isInstanceOf(ClothesExtractionFailedException.class);
    }
  }
}
