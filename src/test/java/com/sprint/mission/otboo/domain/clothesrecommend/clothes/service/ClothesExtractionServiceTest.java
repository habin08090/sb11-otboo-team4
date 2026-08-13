package com.sprint.mission.otboo.domain.clothesrecommend.clothes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
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
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Spy;

@ExtendWith(MockitoExtension.class)
class ClothesExtractionServiceTest {

  @InjectMocks
  ClothesExtractionService clothesExtractionService;

  @Mock
  PurchasePageClient purchasePageClient;

  @Mock
  PurchasePageParser purchasePageParser;

  @Mock
  LlmClient llmClient;

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

      PurchasePageResponse ogResult = new PurchasePageResponse(
          "데님 자켓",
          "https://image.musinsa.com/goods/001.jpg",
          "클래식한 데님 자켓",
          "무신사"
      );

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

      ReflectionTestUtils.setField(clothesExtractionService, "llmApiKey", "test-key");
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
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("URL이 null이면 예외가 발생한다")
    void URL이_null이면_예외가_발생한다() {
      // when & then
      assertThatThrownBy(() -> clothesExtractionService.extractByUrl(null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
