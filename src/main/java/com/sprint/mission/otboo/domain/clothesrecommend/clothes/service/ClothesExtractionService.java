package com.sprint.mission.otboo.domain.clothesrecommend.clothes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.external.llm.LlmClient;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionRequest.LlmMessage;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionResponse;
import com.sprint.mission.otboo.external.purchase.PurchasePageClient;
import com.sprint.mission.otboo.external.purchase.PurchasePageParser;
import com.sprint.mission.otboo.external.purchase.dto.PurchasePageResponse;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ClothesExtractionService {

  private final PurchasePageClient purchasePageClient;
  private final PurchasePageParser purchasePageParser;
  private final LlmClient llmClient;
  private final ObjectMapper objectMapper;

  @Value("${external.llm.api-key}")
  private String llmApiKey;

  public ClothesDto extractByUrl(String url) {
    validateUrl(url);

    String html = purchasePageClient.fetchPage(URI.create(url));
    PurchasePageResponse ogResult = purchasePageParser.parse(html);

    if (!ogResult.isEmpty()) {
      return buildFromOgResult(ogResult);
    }

    log.info("OG 태그 파싱 실패, LLM 폴백 시도: {}", url);
    return extractByLlm(html);
  }

  private void validateUrl(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("URL은 필수입니다.");
    }
  }

  private ClothesDto buildFromOgResult(PurchasePageResponse ogResult) {
    return new ClothesDto(
        null,
        null,
        ogResult.title(),
        ogResult.imageUrl(),
        null,
        List.of()
    );
  }

  private ClothesDto extractByLlm(String html) {
    String systemPrompt = """
        당신은 쇼핑몰 상품 페이지의 HTML을 분석하는 전문가입니다.
        HTML에서 의상 정보를 추출하여 아래 JSON 형식으로만 응답하세요.
        다른 텍스트는 절대 포함하지 마세요.
        {"name": "상품명", "imageUrl": "이미지URL"}
        """;

    String userPrompt = "다음 HTML에서 의상 정보를 추출하세요:\n"
        + html.substring(0, Math.min(html.length(), 3000));

    LlmExtractionRequest request = new LlmExtractionRequest(
        "google/gemma-3-1b-it:free",
        List.of(
            new LlmMessage("system", systemPrompt),
            new LlmMessage("user", userPrompt)
        )
    );

    LlmExtractionResponse response = llmClient.extract("Bearer " + llmApiKey, request);
    return parseLlmResponse(response);
  }

  private ClothesDto parseLlmResponse(LlmExtractionResponse response) {
    String content = response.getContent();
    if (content == null || content.isBlank()) {
      throw new RuntimeException("LLM 응답이 비어있습니다.");
    }

    try {
      JsonNode node = objectMapper.readTree(content);
      String name = node.has("name") ? node.get("name").asText(null) : null;
      String imageUrl = node.has("imageUrl") ? node.get("imageUrl").asText(null) : null;

      return new ClothesDto(null, null, name, imageUrl, null, List.of());
    } catch (Exception e) {
      log.error("LLM 응답 파싱 실패: {}", content, e);
      throw new RuntimeException("LLM 응답을 파싱할 수 없습니다.", e);
    }
  }
}
