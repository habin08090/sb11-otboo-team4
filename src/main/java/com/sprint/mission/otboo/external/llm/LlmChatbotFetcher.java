package com.sprint.mission.otboo.external.llm;

import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest.LlmMessage;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWeather;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import feign.FeignException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmChatbotFetcher {

  private static final String SYSTEM_PROMPT = """
      당신은 옷에 대해 조언하는 도우미입니다.
      옷과 조금이라도 관련이 있으면 무엇이든 답하세요. 무엇을 입을지, 날씨에 맞는 차림, 코디와 색 조합,
      소재, 빨래와 세탁, 건조, 다림질, 보관, 냄새와 얼룩, 수선, 구매 조언까지 전부 포함합니다.
      여기 적히지 않은 표현으로 물어도 옷 이야기라면 답하세요.
      일반적인 질문에는 옷장에 없는 옷을 언급해도 됩니다.
      옷장을 참고하는 경우는 "오늘 뭐 입을까"처럼 무엇을 입을지 대신 골라 달라고 할 때뿐입니다.
      그때는 옷장에서 상황과 날씨에 어울리는 옷만 고르고, 어울리지 않는 옷은 옷장에 있어도 추천하지 마세요.
      마땅한 옷이 없으면 없다고 말한 뒤 어떤 옷이 필요한지 알려주세요.
      반대로 사용자가 특정 옷이나 색 조합을 말하며 어떤지 물으면, 그 조합 자체에 대해 의견을 말하세요.
      이때는 옷장에 없더라도 상관없으니 옷장 이야기를 꺼내지 말고, 어울리는지와 그 이유를 답하세요.
      면접, 결혼식처럼 격식이 필요한 자리나 무엇을 사면 좋을지 묻는 질문에는 옷장에 없는 옷을 권해도 됩니다.
      날씨 정보가 주어지지 않으면 사용자가 질문에 적은 날씨를 참고하세요.
      옷차림과 전혀 무관한 질문(코딩, 뉴스, 계산 등)에만 "옷차림 관련 질문만 도와드릴 수 있어요"라고 답하세요.
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
        %s추위 민감도: %d (1: 더위 많이 탐 ~ 5: 추위 많이 탐)

        사용자 옷장:
        %s

        질문: %s
        """.formatted(
        describeWeather(context.weather()), context.sensitivity(),
        describeWardrobe(context.wardrobe()), context.question());
  }

  private String describeWeather(LlmChatbotWeather weather) {
    if (weather == null) {
      return "";
    }
    return """
        기온: %.1f도
        강수: %s
        바람: %s
        """.formatted(weather.temperature(), weather.precipitationType(), weather.windStrength());
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
