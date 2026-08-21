package com.sprint.mission.otboo.external.llm;

import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import com.sprint.mission.otboo.external.llm.dto.LlmSelectedClothes;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class LlmRecommendationParser {

  private final ObjectMapper objectMapper;

  public LlmRecommendationParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public LlmSelectedClothes parse(LlmChatResponse response) {
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw LlmApiException.parseFailed();
    }
    String content = response.getContent();
    if (content == null || content.isBlank()) {
      throw LlmApiException.parseFailed();
    }

    try {
      String json = stripCodeFence(content);
      JsonNode node = objectMapper.readTree(json);
      if (!node.has("clothesIds") || !node.get("clothesIds").isArray()) {
        throw LlmApiException.parseFailed();
      }

      List<UUID> clothesIds = node.get("clothesIds").valueStream()
          .map(idNode -> UUID.fromString(idNode.asString()))
          .toList();

      if (clothesIds.isEmpty()) {
        throw LlmApiException.parseFailed();
      }

      return new LlmSelectedClothes(clothesIds);
    } catch (LlmApiException e) {
      throw e;
    } catch (Exception e) {
      throw LlmApiException.parseFailed();
    }
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
