package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotAnswerFailedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit.ChatbotRateLimiter;
import com.sprint.mission.otboo.external.llm.LlmChatbotFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotAnswerServiceTest {

  private static final String QUESTION = "오늘 뭐 입을까?";

  @InjectMocks
  ChatbotAnswerService chatbotAnswerService;

  @Mock
  ChatbotRateLimiter chatbotRateLimiter;
  @Mock
  ChatbotContextProvider chatbotContextProvider;
  @Mock
  LlmChatbotFetcher llmChatbotFetcher;

  private static LlmChatbotContext context() {
    return new LlmChatbotContext(QUESTION, null, 3, List.of());
  }

  @Nested
  @DisplayName("답변 생성")
  class Answer {

    @Test
    @DisplayName("수집한_컨텍스트로_LLM_답변을_반환한다")
    void 수집한_컨텍스트로_LLM_답변을_반환한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID weatherId = UUID.randomUUID();
      LlmChatbotContext context = context();

      given(chatbotContextProvider.collect(userId, QUESTION, weatherId)).willReturn(context);
      given(llmChatbotFetcher.answer(context)).willReturn("셔츠를 입어보세요.");

      // when
      String result = chatbotAnswerService.answer(userId, QUESTION, weatherId);

      // then
      assertThat(result).isEqualTo("셔츠를 입어보세요.");
    }

    @Test
    @DisplayName("사용량_제한을_초과하면_컨텍스트도_모으지_않는다")
    void 사용량_제한을_초과하면_컨텍스트도_모으지_않는다() {
      // given
      UUID userId = UUID.randomUUID();
      willThrow(ChatbotRateLimitExceededException.withUserId(userId, 10))
          .given(chatbotRateLimiter).check(userId);

      // when & then
      assertThatThrownBy(() -> chatbotAnswerService.answer(userId, QUESTION, null))
          .isInstanceOf(ChatbotRateLimitExceededException.class);
      verifyNoInteractions(chatbotContextProvider, llmChatbotFetcher);
    }

    @Test
    @DisplayName("LLM_호출이_실패하면_ChatbotAnswerFailedException으로_wrap한다")
    void LLM_호출이_실패하면_ChatbotAnswerFailedException으로_wrap한다() {
      // given
      UUID userId = UUID.randomUUID();

      given(chatbotContextProvider.collect(userId, QUESTION, null)).willReturn(context());
      given(llmChatbotFetcher.answer(any(LlmChatbotContext.class)))
          .willThrow(LlmApiException.parseFailed());

      // when & then
      assertThatThrownBy(() -> chatbotAnswerService.answer(userId, QUESTION, null))
          .isInstanceOf(ChatbotAnswerFailedException.class);
    }
  }
}
