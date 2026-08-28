package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.dto.ChatbotAskRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotException;
import com.sprint.mission.otboo.domain.social.directmessage.config.DirectMessageRedisConfig;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageBroadcast;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
import com.sprint.mission.otboo.domain.social.directmessage.util.StompDestinationUtil;
import com.sprint.mission.otboo.global.init.ChatBotInitializer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * 챗봇 대화 한 번(질문 → 답변)을 처리한다.
 *
 * <p>질문을 답변보다 먼저 저장·발행한다. 화면은 서버가 되돌려준 메시지를 받아 말풍선을 그리므로, 이 순서가 아니면 사용자가 자기 말풍선조차 LLM 응답을 기다린 뒤에 보게
 * 된다.
 *
 * <p>저장·전파는 기존 DM 것을 그대로 쓴다. 봇이 사용자 계정이라 DM 인가와 화면이 그대로 통한다.
 *
 * <p>클래스에 트랜잭션을 걸지 않는다. LLM 호출이 2~4초 걸리는데 그 구간을 트랜잭션으로 감싸면 응답을 기다리는 동안 DB 커넥션을 붙잡고 있게 된다. 저장은
 * {@link DirectMessageService#send}가 자체 트랜잭션으로 처리한다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ChatbotConversationService {

  /** 계정을 만드는 쪽과 같은 값을 봐야 하므로 상수를 그대로 참조한다. 양쪽에 UUID를 따로 적어두면 한쪽만 바뀔 때 조용히 어긋난다. */
  private static final UUID CHATBOT_USER_ID = ChatBotInitializer.CHAT_BOT_USER_ID;

  private final ChatbotAnswerService chatbotAnswerService;
  private final DirectMessageService directMessageService;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  public void ask(UUID currentUserId, ChatbotAskRequest request) {
    sendAndBroadcast(
        new DirectMessageSendRequest(request.senderId(), CHATBOT_USER_ID, request.content()),
        currentUserId);

    String answer = answerOrGuide(currentUserId, request);

    sendAndBroadcast(
        new DirectMessageSendRequest(CHATBOT_USER_ID, currentUserId, answer), CHATBOT_USER_ID);
  }

  /**
   * 답변 생성이 실패해도 예외를 밖으로 던지지 않는다. STOMP ERROR 프레임은 명세상 연결 종료를 수반해, 답변 하나가 실패했다고 소켓이 끊긴다. 대신 봇이 안내
   * 메시지를 DM으로 보낸다.
   */
  private String answerOrGuide(UUID currentUserId, ChatbotAskRequest request) {
    try {
      return chatbotAnswerService.answer(currentUserId, request.content(), request.weatherId());
    } catch (ChatbotException e) {
      log.warn("챗봇 답변을 만들지 못해 안내 메시지를 보낸다 userId={}", currentUserId, e);
      return e.getMessage();
    }
  }

  private void sendAndBroadcast(DirectMessageSendRequest request, UUID currentUserId) {
    DirectMessageDto saved = directMessageService.send(request, currentUserId);

    String destination = StompDestinationUtil.directMessageDestination(
        saved.sender().userId(), saved.receiver().userId());
    stringRedisTemplate.convertAndSend(DirectMessageRedisConfig.DM_CHANNEL,
        objectMapper.writeValueAsString(new DirectMessageBroadcast(destination, saved)));
  }
}
