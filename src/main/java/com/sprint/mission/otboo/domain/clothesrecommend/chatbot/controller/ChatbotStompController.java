package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.controller;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.dto.ChatbotAskRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotUnauthorizedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service.ChatbotAnswerService;
import com.sprint.mission.otboo.domain.social.directmessage.config.DirectMessageRedisConfig;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageBroadcast;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
import com.sprint.mission.otboo.domain.social.directmessage.util.StompDestinationUtil;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.ObjectMapper;

/**
 * 챗봇 전용 STOMP 진입점.
 *
 * <p>저장·전파·구독·조회는 전부 기존 DM 것을 그대로 쓴다. 봇이 사용자 계정이라 DM 인가와 화면이 그대로 통한다.
 *
 * <p>질문을 답변보다 먼저 저장·발행한다 — 화면은 서버가 되돌려준 메시지를 받아 말풍선을 그리므로, 질문을 먼저 발행하지 않으면 사용자가 자기 말풍선조차 LLM 응답을
 * 기다린 뒤에 보게 된다.
 */
@Slf4j
@Controller
public class ChatbotStompController {

  private final ChatbotAnswerService chatbotAnswerService;
  private final DirectMessageService directMessageService;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final UUID chatbotUserId;

  public ChatbotStompController(ChatbotAnswerService chatbotAnswerService,
      DirectMessageService directMessageService, StringRedisTemplate stringRedisTemplate,
      ObjectMapper objectMapper,
      @Value("${chatbot.user-id:00000000-0000-0000-0000-000000000001}") UUID chatbotUserId) {
    this.chatbotAnswerService = chatbotAnswerService;
    this.directMessageService = directMessageService;
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.chatbotUserId = chatbotUserId;
  }

  @MessageMapping("/chatbot_ask")
  public void ask(@Valid ChatbotAskRequest request, Principal principal) {
    UUID currentUserId = extractUserId(principal);

    sendAndBroadcast(
        new DirectMessageSendRequest(request.senderId(), chatbotUserId, request.content()),
        currentUserId);

    String answer = answerOrGuide(currentUserId, request);

    sendAndBroadcast(
        new DirectMessageSendRequest(chatbotUserId, currentUserId, answer), chatbotUserId);
  }

  /**
   * 답변 생성이 실패해도 STOMP ERROR 프레임을 던지지 않는다. ERROR 프레임은 명세상 연결 종료를 수반해, 답변 하나가 실패했다고 소켓이 끊긴다. 대신 봇이 안내
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

  private UUID extractUserId(Principal principal) {
    if (!(principal instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
      throw ChatbotUnauthorizedException.withNone();
    }
    return userPrincipal.userId();
  }
}
