package com.sprint.mission.otboo.external.llm.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Choice;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse.Message;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LlmChatResponseTest {

  @Nested
  @DisplayName("본문 추출")
  class GetContent {

    @Test
    @DisplayName("정상_응답이면_content를_반환한다")
    void 정상_응답이면_content를_반환한다() {
      LlmChatResponse response =
          new LlmChatResponse(List.of(new Choice(new Message("assistant", "안녕"))));

      assertThat(response.getContent()).isEqualTo("안녕");
    }

    @Test
    @DisplayName("choices가_비어있으면_null을_반환한다")
    void choices가_비어있으면_null을_반환한다() {
      LlmChatResponse response = new LlmChatResponse(List.of());

      assertThat(response.getContent()).isNull();
    }

    @Test
    @DisplayName("첫_번째_Choice가_null이면_예외_대신_null을_반환한다")
    void 첫_번째_Choice가_null이면_예외_대신_null을_반환한다() {
      LlmChatResponse response = new LlmChatResponse(Collections.singletonList(null));

      assertThat(response.getContent()).isNull();
    }

    @Test
    @DisplayName("Message가_null이면_예외_대신_null을_반환한다")
    void Message가_null이면_예외_대신_null을_반환한다() {
      LlmChatResponse response = new LlmChatResponse(List.of(new Choice(null)));

      assertThat(response.getContent()).isNull();
    }
  }
}
