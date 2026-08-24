package com.sprint.mission.otboo.domain.social.directmessage.controller;

import com.sprint.mission.otboo.domain.social.directmessage.config.DirectMessageRedisConfig;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageBroadcast;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.exception.DirectMessageUnauthorizedException;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
import com.sprint.mission.otboo.domain.social.directmessage.util.StompDestinationUtil;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageStompController {

  private final DirectMessageService directMessageService;
  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;

  @MessageMapping("/direct-messages_send")
  public void send(@Valid DirectMessageSendRequest request, Principal principal) {
    UUID currentUserId = extractUserId(principal);

    DirectMessageDto saved = directMessageService.send(request, currentUserId);
    log.info("DM 발행 완료: dmId={}", saved.id());

    String destination = StompDestinationUtil.directMessageDestination(
        saved.sender().userId(), saved.receiver().userId());
    stringRedisTemplate.convertAndSend(DirectMessageRedisConfig.DM_CHANNEL,
        objectMapper.writeValueAsString(new DirectMessageBroadcast(destination, saved)));
  }

  private UUID extractUserId(Principal principal) {
    if (!(principal instanceof Authentication authentication)
        || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
      throw DirectMessageUnauthorizedException.withNone();
    }
    return userPrincipal.userId();
  }
}
