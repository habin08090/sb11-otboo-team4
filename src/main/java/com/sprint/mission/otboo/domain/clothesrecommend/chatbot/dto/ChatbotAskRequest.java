package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 챗봇에게 보내는 질문.
 *
 * <p>DM 전송 페이로드와 같은 형태에 {@code weatherId}만 더했다 — 화면이 이미 들고 있는 날씨를 그대로 실어 보내면 서버가 좌표로 날씨를 다시 조회하지 않아도
 * 된다. 값이 없으면 날씨 없이 답한다.
 *
 * <p>{@code receiverId}는 기존 DM 전송 코드와 형태를 맞추려고 받을 뿐, 서버는 이 값을 신뢰하지 않고 봇 계정으로 덮어쓴다.
 */
public record ChatbotAskRequest(
    @NotNull UUID senderId,
    UUID receiverId,
    @NotBlank @Size(max = 500) String content,
    UUID weatherId
) {

}
