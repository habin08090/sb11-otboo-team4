package com.sprint.mission.otboo.domain.clothesrecommend.clothes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception.ClothesExtractionBadRequestException;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception.ClothesExtractionFailedException;
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
import com.sprint.mission.otboo.external.llm.LlmProperties;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClothesExtractionService {

  private static final int MAX_BODY_TEXT_CHARS = 3000;
  private final PurchasePageClient purchasePageClient;
  private static final Set<String> ALLOWED_HOSTS = Set.of(
      "www.musinsa.com",
      "musinsa.com",
      "store.musinsa.com",
      "www.29cm.co.kr",
      "29cm.co.kr",
      "www.wconcept.co.kr",
      "wconcept.co.kr",
      "zigzag.kr",
      "www.zigzag.kr"
  );
  private final PurchasePageParser purchasePageParser;
  private final LlmClient llmClient;

  private final LlmProperties llmProperties;
  private final ObjectMapper objectMapper;

  public ClothesDto extractByUrl(String url) {
    validateUrl(url);

    String html = fetchPage(url);
    PurchasePageResponse ogResult = purchasePageParser.parse(html);

    if (!ogResult.isEmpty()) {
      return buildFromOgResult(ogResult);
    }

    log.info("OG 태그 파싱 실패, LLM 폴백 시도: {}", url);
    return extractByLlm(url, html);
  }

  private void validateUrl(String url) {
    if (url == null || url.isBlank()) {
      throw ClothesExtractionBadRequestException.invalidUrl(url);
    }

    try {
      URI uri = URI.create(url);
      String scheme = uri.getScheme();
      if (scheme == null || !scheme.equals("https")) {
        throw ClothesExtractionBadRequestException.invalidUrl(url);
      }

      String host = uri.getHost();
      if (host == null || !ALLOWED_HOSTS.contains(host)) {
        throw ClothesExtractionBadRequestException.invalidUrl(url);
      }
    } catch (ClothesExtractionBadRequestException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      throw ClothesExtractionBadRequestException.invalidUrl(url);
    }
  }

  private String fetchPage(String url) {
    try {
      return purchasePageClient.fetchPage(URI.create(url));
    } catch (Exception e) {
      log.error("구매 페이지 요청 실패: url={}, 예외={}", url, e.getClass().getSimpleName());
      throw ClothesExtractionFailedException.pageFetchFailed(url, e);
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

  private ClothesDto extractByLlm(String url, String html) {
    String systemPrompt = """
        당신은 쇼핑몰 상품 페이지의 HTML을 분석하는 전문가입니다.
        HTML에서 의상 정보를 추출하여 아래 JSON 형식으로만 응답하세요.
        다른 텍스트는 절대 포함하지 마세요.
        {"name": "상품명", "imageUrl": "이미지URL"}
        """;

    String bodyText = extractBodyText(html);
    String userPrompt = "다음 텍스트에서 의상 정보를 추출하세요:\n"
        + bodyText.substring(0, Math.min(bodyText.length(), MAX_BODY_TEXT_CHARS));

    LlmExtractionRequest request = new LlmExtractionRequest(
        llmProperties.model(),
        List.of(
            new LlmMessage("system", systemPrompt),
            new LlmMessage("user", userPrompt)
        )
    );

    try {
      LlmExtractionResponse response = llmClient.extract("Bearer " + llmProperties.apiKey(), request);
      return parseLlmResponse(response);
    } catch (ClothesExtractionFailedException e) {
      throw e;
    } catch (Exception e) {
      log.error("LLM 호출 실패: url={}, 예외={}", url, e.getClass().getSimpleName());
      throw ClothesExtractionFailedException.llmCallFailed(e);
    }
  }

  private ClothesDto parseLlmResponse(LlmExtractionResponse response) {
    String content = response.getContent();
    if (content == null || content.isBlank()) {
      log.error("LLM 응답이 비어있음");
      throw ClothesExtractionFailedException.llmParseFailed();
    }

    try {
      String json = stripCodeFence(content);
      JsonNode node = objectMapper.readTree(json);
      String name = node.has("name") ? node.get("name").asText(null) : null;
      String imageUrl = node.has("imageUrl") ? node.get("imageUrl").asText(null) : null;

      return new ClothesDto(null, null, name, imageUrl, null, List.of());
    } catch (Exception e) {
      log.error("LLM 응답 파싱 실패: 응답 길이={}, 예외={}", content.length(), e.getClass().getSimpleName());
      throw ClothesExtractionFailedException.llmParseFailed();
    }
  }
  private String extractBodyText(String html) {
    var doc = Jsoup.parse(html);
    doc.select("script, style").remove();
    return doc.body() != null ? doc.body().text() : "";
  }

  private String stripCodeFence(String content) {
    String stripped = content.strip();
    if (stripped.startsWith("```")) {
      stripped = stripped.replaceFirst("```\\w*\\n?", "");
      if (stripped.endsWith("```")) {
        stripped = stripped.substring(0, stripped.lastIndexOf("```"));
      }
    }
    return stripped.strip();
  }
}
