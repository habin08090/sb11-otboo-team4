package com.sprint.mission.otboo.external.llm;

import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest.LlmMessage;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import feign.FeignException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmChatbotFetcher {

  private static final String SYSTEM_PROMPT = """
      당신은 옷차림을 조언하는 도우미입니다.
      의상, 날씨, 코디에 대해서만 답하세요.
      그 외 질문에는 "옷차림 관련 질문만 도와드릴 수 있어요"라고 답하세요.
      사용자의 옷장에 없는 옷은 추천하지 마세요.
      마크다운 없이 3문장 이내의 대화체로 답하세요.
      """;
  private static final String EMPTY_WARDROBE = "(등록된 의상이 없습니다)";

  private final LlmClient llmClient;
  private final String model;

  public LlmChatbotFetcher(LlmClient llmClient, @Value("${external.llm.model}") String model) {
    this.llmClient = llmClient;
    this.model = model;
  }

  public String answer(LlmChatbotContext context) {
    LlmChatRequest request = new LlmChatRequest(
        model,
        List.of(
            new LlmMessage("system", SYSTEM_PROMPT),
            new LlmMessage("user", buildUserPrompt(context))
        )
    );

    LlmChatResponse response;
    try {
      response = llmClient.chat(request);
    } catch (FeignException e) {
      throw LlmApiException.callFailed(e);
    }

    String content = response.getContent();
    if (content == null || content.isBlank()) {
      throw LlmApiException.parseFailed();
    }
    return content.strip();
  }

  private String buildUserPrompt(LlmChatbotContext context) {
    return """
        기온: %.1f도
        강수: %s
        바람: %s
        추위 민감도: %d (1: 더위 많이 탐 ~ 5: 추위 많이 탐)

        사용자 옷장:
        %s

        질문: %s
        """.formatted(
        context.temperature(), context.precipitationType(), context.windStrength(),
        context.sensitivity(), describeWardrobe(context.wardrobe()), context.question());
  }

  private String describeWardrobe(List<LlmChatbotWardrobeItem> wardrobe) {
    if (wardrobe == null || wardrobe.isEmpty()) {
      return EMPTY_WARDROBE;
    }
    return wardrobe.stream()
        .map(this::describeWardrobeItem)
        .collect(Collectors.joining("\n"));
  }

  private String describeWardrobeItem(LlmChatbotWardrobeItem item) {
    return "- name=%s, type=%s, attributes=%s".formatted(
        item.name(), item.type(), item.attributeSummary());
  }
}
