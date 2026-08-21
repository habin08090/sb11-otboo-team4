package com.sprint.mission.otboo.external.llm.dto;

import java.util.List;

public record LlmChatResponse(
    List<Choice> choices
) {

  public record Choice(
      Message message
  ) {

  }

  public record Message(
      String role,
      String content
  ) {

  }

  public String getContent() {
    if (choices == null || choices.isEmpty()) {
      return null;
    }
    Choice choice = choices.get(0);
    if (choice == null || choice.message() == null) {
      return null;
    }
    return choice.message().content();
  }
}
