package com.sprint.mission.otboo.external.llm;

import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest.LlmMessage;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationCandidate;
import com.sprint.mission.otboo.external.llm.dto.LlmRecommendationContext;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import feign.FeignException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LlmRecommendationFetcher {

  private static final String SYSTEM_PROMPT = """
      당신은 날씨에 맞는 옷차림을 추천하는 스타일리스트입니다.
      주어진 날씨 정보와 후보 의상 목록 중에서 가장 적절한 조합을 선택하세요.
      후보 목록에 없는 clothesId는 절대 포함하지 마세요.
      다른 텍스트 없이 아래 JSON 형식으로만 응답하세요.
      {"clothesIds": ["후보 중에서 선택한 clothesId", ...]}
      """;

  private final LlmClient llmClient;
  private final LlmRecommendationParser llmRecommendationParser;
  private final String model;

  public LlmRecommendationFetcher(LlmClient llmClient,
      LlmRecommendationParser llmRecommendationParser,
      @Value("${external.llm.model}") String model) {
    this.llmClient = llmClient;
    this.llmRecommendationParser = llmRecommendationParser;
    this.model = model;
  }

  public LlmSelectedClothes select(LlmRecommendationContext context) {
    String userPrompt = buildUserPrompt(context);

    LlmChatRequest request = new LlmChatRequest(
        model,
        List.of(
            new LlmMessage("system", SYSTEM_PROMPT),
            new LlmMessage("user", userPrompt)
        )
    );

    LlmChatResponse response;
    try {
      response = llmClient.chat(request);
    } catch (FeignException e) {
      throw LlmApiException.callFailed(e);
    }

    return llmRecommendationParser.parse(response);
  }

  private String buildUserPrompt(LlmRecommendationContext context) {
    String candidateLines = context.candidates().stream()
        .map(this::describeCandidate)
        .collect(Collectors.joining("\n"));

    return """
        기온: %.1f도 (체감 보정: %.1f도)
        강수: %s
        바람: %s
        추위 민감도: %d (1: 더위 많이 탐 ~ 5: 추위 많이 탐)

        후보 의상 목록:
        %s
        """.formatted(
        context.temperature(), context.adjustedTemperature(),
        context.precipitationType(), context.windStrength(), context.sensitivity(),
        candidateLines);
  }

  private String describeCandidate(LlmRecommendationCandidate candidate) {
    return "- clothesId=%s, name=%s, type=%s, attributes=%s".formatted(
        candidate.clothesId(), candidate.name(), candidate.type(),
        candidate.attributeSummary());
  }
}
