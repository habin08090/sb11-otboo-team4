package com.sprint.mission.otboo.external.purchase;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.external.purchase.dto.PurchasePageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PurchasePageParserTest {

  private final PurchasePageParser parser = new PurchasePageParser();

  @Test
  @DisplayName("OG 태그가 모두 있는 HTML → 정상 파싱")
  void parse_withFullOgTags_returnsResponse() {
    // given
    String html = """
        <html>
        <head>
          <meta property="og:title" content="슬림핏 데님 자켓" />
          <meta property="og:image" content="https://image.musinsa.com/images/goods/001.jpg" />
          <meta property="og:description" content="클래식한 데님 자켓입니다." />
          <meta property="og:site_name" content="무신사" />
        </head>
        <body></body>
        </html>
        """;

    // when
    PurchasePageResponse result = parser.parse(html);

    // then
    assertThat(result.title()).isEqualTo("슬림핏 데님 자켓");
    assertThat(result.imageUrl()).isEqualTo("https://image.musinsa.com/images/goods/001.jpg");
    assertThat(result.description()).isEqualTo("클래식한 데님 자켓입니다.");
    assertThat(result.siteName()).isEqualTo("무신사");
    assertThat(result.isEmpty()).isFalse();
  }

  @Test
  @DisplayName("OG 태그가 없는 HTML → 빈 결과")
  void parse_withoutOgTags_returnsEmpty() {
    // given
    String html = """
        <html>
        <head><title>상품 페이지</title></head>
        <body><h1>데님 자켓</h1></body>
        </html>
        """;

    // when
    PurchasePageResponse result = parser.parse(html);

    // then
    assertThat(result.title()).isNull();
    assertThat(result.imageUrl()).isNull();
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("빈 HTML → 빈 결과")
  void parse_withEmptyHtml_returnsEmpty() {
    // given
    String html = "";

    // when
    PurchasePageResponse result = parser.parse(html);

    // then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  @DisplayName("일부 OG 태그만 있는 HTML → 있는 것만 채움")
  void parse_withPartialOgTags_returnsPartial() {
    // given
    String html = """
        <html>
        <head>
          <meta property="og:title" content="오버사이즈 후드티" />
        </head>
        <body></body>
        </html>
        """;

    // when
    PurchasePageResponse result = parser.parse(html);

    // then
    assertThat(result.title()).isEqualTo("오버사이즈 후드티");
    assertThat(result.imageUrl()).isNull();
    assertThat(result.isEmpty()).isFalse();
  }
}
