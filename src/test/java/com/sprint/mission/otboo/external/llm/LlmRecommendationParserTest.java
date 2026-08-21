package com.sprint.mission.otboo.external.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Choice;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Message;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class LlmRecommendationParserTest {

  private final LlmRecommendationParser parser = new LlmRecommendationParser(new ObjectMapper());

  private static LlmChatResponse response(String content) {
    return new LlmChatResponse(List.of(new Choice(new Message("assistant", content))));
  }

  @Nested
  @DisplayName("파싱")
  class Parse {

    @Test
    @DisplayName("코드펜스_없는_JSON을_파싱한다")
    void 코드펜스_없는_JSON을_파싱한다() {
      UUID id1 = UUID.randomUUID();
      UUID id2 = UUID.randomUUID();
      LlmChatResponse response = response(
          "{\"clothesIds\": [\"" + id1 + "\", \"" + id2 + "\"]}");

      LlmSelectedClothes result = parser.parse(response);

      assertThat(result.clothesIds()).containsExactly(id1, id2);
    }

    @Test
    @DisplayName("코드펜스로_감싼_JSON도_파싱한다")
    void 코드펜스로_감싼_JSON도_파싱한다() {
      UUID id = UUID.randomUUID();
      LlmChatResponse response = response(
          "```json\n{\"clothesIds\": [\"" + id + "\"]}\n```");

      LlmSelectedClothes result = parser.parse(response);

      assertThat(result.clothesIds()).containsExactly(id);
    }

    @Test
    @DisplayName("content가_비어있으면_LlmApiException을_던진다")
    void content가_비어있으면_LlmApiException을_던진다() {
      LlmChatResponse response = response("");

      assertThatThrownBy(() -> parser.parse(response))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("JSON이_아니면_LlmApiException을_던진다")
    void JSON이_아니면_LlmApiException을_던진다() {
      LlmChatResponse response = response("이건 JSON이 아닙니다");

      assertThatThrownBy(() -> parser.parse(response))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("clothesIds_필드가_없으면_LlmApiException을_던진다")
    void clothesIds_필드가_없으면_LlmApiException을_던진다() {
      LlmChatResponse response = response("{\"reason\": \"no selection\"}");

      assertThatThrownBy(() -> parser.parse(response))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("UUID_형식이_아닌_값이_섞여있으면_LlmApiException을_던진다")
    void UUID_형식이_아닌_값이_섞여있으면_LlmApiException을_던진다() {
      LlmChatResponse response = response("{\"clothesIds\": [\"not-a-uuid\"]}");

      assertThatThrownBy(() -> parser.parse(response))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("응답이_null이면_LlmApiException을_던진다")
    void 응답이_null이면_LlmApiException을_던진다() {
      assertThatThrownBy(() -> parser.parse(null))
          .isInstanceOf(LlmApiException.class);
    }

    @Test
    @DisplayName("clothesIds가_빈_배열이면_LlmApiException을_던진다")
    void clothesIds가_빈_배열이면_LlmApiException을_던진다() {
      LlmChatResponse response = response("{\"clothesIds\": []}");

      assertThatThrownBy(() -> parser.parse(response))
          .isInstanceOf(LlmApiException.class);
    }
  }
}
