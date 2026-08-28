package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.controller;

import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.dto.ChatbotAskRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotUnauthorizedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service.ChatbotConversationService;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

/**
 * 챗봇 전용 STOMP 진입점.
 *
 * <p>대화 처리는 {@link ChatbotConversationService}가 맡고, 여기서는 인증 주체만 꺼내 넘긴다.
 */
@Controller
@RequiredArgsConstructor
public class ChatbotStompController {

  private final ChatbotConversationService chatbotConversationService;

  @MessageMapping("/chatbot_ask")
  public void ask(@Valid ChatbotAskRequest request, Principal principal) {
    chatbotConversationService.ask(extractUserId(principal), request);
  }

  private UUID extractUserId(Principal principal) {
    if (!(principal instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
      throw ChatbotUnauthorizedException.withNone();
    }
    return userPrincipal.userId();
  }
}
