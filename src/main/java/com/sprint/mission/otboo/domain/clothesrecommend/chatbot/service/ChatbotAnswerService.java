package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotAnswerFailedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit.ChatbotRateLimiter;
import com.sprint.mission.otboo.external.llm.LlmChatbotFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.exception.LlmException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 옷차림 질문에 대한 LLM 답변을 만든다.
 *
 * <p>트랜잭션을 걸지 않는다. 컨텍스트 조회는 {@link ChatbotContextProvider}가 자체 읽기 트랜잭션에서 끝내고, 2~4초 걸리는 LLM 호출은 그 밖에서
 * 수행해 DB 커넥션을 붙잡지 않는다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatbotAnswerService {

  private final ChatbotRateLimiter chatbotRateLimiter;
  private final ChatbotContextProvider chatbotContextProvider;
  private final LlmChatbotFetcher llmChatbotFetcher;

  public String answer(UUID userId, String question, UUID weatherId) {
    chatbotRateLimiter.check(userId);

    LlmChatbotContext context = chatbotContextProvider.collect(userId, question, weatherId);

    try {
      return llmChatbotFetcher.answer(context);
    } catch (LlmException e) {
      log.error("챗봇 답변 생성 실패 userId={}", userId, e);
      throw ChatbotAnswerFailedException.llmCallFailed(userId, e);
    }
  }
}
