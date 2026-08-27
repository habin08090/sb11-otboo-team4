package com.sprint.mission.otboo.external.llm.dto;

import java.util.List;

/**
 * 챗봇 답변 생성에 쓰이는 입력 묶음.
 *
 * <p>{@code weather}는 없을 수 있다 — 화면이 이미 들고 있는 날씨를 그대로 받아 쓰는 구조라, 사용자가 날씨를 아직 불러오지 않은 상태에서 질문하면 비어 있다.
 * 그때는 날씨 없이 답하고, 사용자가 질문에 직접 적은 날씨를 참고하게 한다.
 */
public record LlmChatbotContext(
    String question,
    LlmChatbotWeather weather,
    int sensitivity,
    List<LlmChatbotWardrobeItem> wardrobe
) {

}
