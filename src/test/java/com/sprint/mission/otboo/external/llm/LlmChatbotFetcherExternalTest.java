package com.sprint.mission.otboo.external.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWeather;
import com.sprint.mission.otboo.global.testcontainers.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 실제 LLM이 가드 프롬프트를 지키는지 확인한다. 답변은 매번 달라지므로 문구를 단정하지 않고, 지켜야 할 최소 조건만 검증하고 본문은 로그로 남긴다.
 */
@Tag("external")
@SpringBootTest
class LlmChatbotFetcherExternalTest extends IntegrationTestSupport {

  private static final Logger log = LoggerFactory.getLogger(LlmChatbotFetcherExternalTest.class);

  @Autowired
  private LlmChatbotFetcher llmChatbotFetcher;

  static FixtureMonkey fixtureMonkey;

  @BeforeAll
  static void setUpFixtureMonkey() {
    fixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
        .plugin(new JakartaValidationPlugin())
        .build();
  }

  private static LlmChatbotWardrobeItem wardrobeItem(String name, ClothesType type,
      String attributeSummary) {
    return fixtureMonkey.giveMeBuilder(LlmChatbotWardrobeItem.class)
        .set("name", name)
        .set("type", type)
        .set("attributeSummary", attributeSummary)
        .sample();
  }

  private static LlmChatbotContext contextOf(String question, LlmChatbotWeather weather) {
    return fixtureMonkey.giveMeBuilder(LlmChatbotContext.class)
        .set("question", question)
        .set("weather", weather)
        .set("sensitivity", 3)
        .set("wardrobe", List.of(
            wardrobeItem("리넨 셔츠", ClothesType.TOP, "두께감=얇음, 색상=흰색"),
            wardrobeItem("치노 반바지", ClothesType.BOTTOM, "색상=베이지"),
            wardrobeItem("가죽 재킷", ClothesType.OUTER, "두께감=두꺼움, 색상=검정")))
        .sample();
  }

  @Nested
  @DisplayName("실제 LLM 답변")
  class Answer {

    @Test
    @DisplayName("옷장_기반_추천_요청에는_옷장에_있는_옷으로_답한다")
    void 옷장_기반_추천_요청에는_옷장에_있는_옷으로_답한다() {
      // given
      LlmChatbotContext context = contextOf("오늘 뭐 입을까?",
          new LlmChatbotWeather(28.0, PrecipitationType.NONE, WindStrength.WEAK));

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[옷장 추천] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).containsAnyOf("리넨 셔츠", "치노 반바지", "가죽 재킷");
    }

    @Test
    @DisplayName("날씨를_질문에_적으면_날씨_정보_없이도_그_날씨로_답한다")
    void 날씨를_질문에_적으면_날씨_정보_없이도_그_날씨로_답한다() {
      // given
      LlmChatbotContext context = contextOf("오늘 28도에 흐린 날씨인데 뭘 입으면 좋을까?", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[질문 속 날씨] {}", answer);
      assertThat(answer).isNotBlank();
    }

    @Test
    @DisplayName("일반적인_코디_질문에는_옷장에_얽매이지_않고_답한다")
    void 일반적인_코디_질문에는_옷장에_얽매이지_않고_답한다() {
      // given
      LlmChatbotContext context = contextOf("초록색 티셔츠에 어울리는 바지 색은 뭐야?", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[코디 상식] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("옷_관리_질문에도_답한다")
    void 옷_관리_질문에도_답한다() {
      // given
      LlmChatbotContext context = contextOf("린넨 옷 세탁은 어떻게 하는 게 좋을까?", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[옷 관리] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("옷장에_없는_옷_조합을_물어도_옷장_이야기_없이_의견을_답한다")
    void 옷장에_없는_옷_조합을_물어도_옷장_이야기_없이_의견을_답한다() {
      // given
      LlmChatbotContext context = contextOf("빨간 티에 청바지 어때?",
          new LlmChatbotWeather(28.0, PrecipitationType.NONE, WindStrength.WEAK));

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[옷 조합 의견] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷장에 없");
    }

    @Test
    @DisplayName("세탁을_빨래라고_물어도_답한다")
    void 세탁을_빨래라고_물어도_답한다() {
      // given
      LlmChatbotContext context = contextOf("면티 빨래 어떻게 하는 게 좋을까?", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[빨래] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("옷_냄새처럼_목록에_없는_표현으로_물어도_답한다")
    void 옷_냄새처럼_목록에_없는_표현으로_물어도_답한다() {
      // given
      LlmChatbotContext context = contextOf("옷에서 쉰내 나는데 어떡하지?", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[냄새] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("상황에_맞는_옷차림_질문에는_옷장에_없는_옷도_권한다")
    void 상황에_맞는_옷차림_질문에는_옷장에_없는_옷도_권한다() {
      // given
      LlmChatbotContext context = contextOf("내일 면접인데 뭐 입고 가는 게 좋을까?", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[TPO] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("무엇을_살지_묻는_질문에도_답한다")
    void 무엇을_살지_묻는_질문에도_답한다() {
      // given
      LlmChatbotContext context = contextOf("가을에 새로 살 만한 아이템 추천해줘.", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[구매 상담] {}", answer);
      assertThat(answer).isNotBlank();
      assertThat(answer).doesNotContain("옷차림 관련 질문만 도와드릴 수 있어요");
    }

    @Test
    @DisplayName("옷차림과_무관한_질문은_거절한다")
    void 옷차림과_무관한_질문은_거절한다() {
      // given
      LlmChatbotContext context = contextOf("파이썬으로 퀵소트 코드 짜줘.", null);

      // when
      String answer = llmChatbotFetcher.answer(context);

      // then
      log.info("[무관한 질문] {}", answer);
      assertThat(answer).contains("옷차림 관련 질문만 도와드릴 수 있어요");
    }
  }
}
