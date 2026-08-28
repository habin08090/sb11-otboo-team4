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
      주어진 날씨 정보를 보고, 후보 의상 목록에서 그 날씨에 입을 만한 의상을 골라주세요.
      완성된 한 벌이 아니라 추천할 만한 의상들을 모아주세요.

      규칙:
      1. 후보 목록에 등장하는 모든 type(종류)을 빠뜨리지 마세요. 신발, 가방, 모자처럼
         눈에 덜 띄는 종류도 반드시 포함해야 합니다.
      2. 각 type마다 2~3벌씩 골라주세요. 그 type에 후보가 1벌뿐일 때만 1벌을 고릅니다.
         한 벌만 고르면 사용자가 매번 같은 옷을 추천받게 되므로 반드시 여러 벌을 고르세요.
      3. 날씨에 어울리지 않는 의상은 같은 type의 다른 의상으로 대신 골라주세요.
         그 type의 모든 후보가 어울리지 않더라도, 그중 가장 나은 것을 골라 type을 비우지 마세요.
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
