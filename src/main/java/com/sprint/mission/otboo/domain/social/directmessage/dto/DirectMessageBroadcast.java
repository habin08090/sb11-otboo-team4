package com.sprint.mission.otboo.domain.social.directmessage.dto;

/**
 * 인스턴스 간 DM 전파용 페이로드.
 *
 * <p>구독한 인스턴스가 어느 destination으로 보낼지 알아야 하므로 함께 담는다.
 */
public record DirectMessageBroadcast(
    String destination,
    DirectMessageDto message
) {

}
