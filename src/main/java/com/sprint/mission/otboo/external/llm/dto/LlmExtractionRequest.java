package com.sprint.mission.otboo.external.llm.dto;

import java.util.List;

public record LlmExtractionRequest(
    String model,
    List<LlmMessage> messages
) {

  public record LlmMessage(
      String role,
      String content
  ) {

  }
}
