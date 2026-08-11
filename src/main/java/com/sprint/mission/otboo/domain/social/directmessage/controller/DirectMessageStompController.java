package com.sprint.mission.otboo.domain.social.directmessage.controller;

import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
import com.sprint.mission.otboo.domain.social.directmessage.util.StompDestinationUtil;
import com.sprint.mission.otboo.global.dto.ErrorResponse;
import com.sprint.mission.otboo.global.exception.OtbooException;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageStompController {

  private final DirectMessageService directMessageService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/direct-messages_send")
  public void send(@Valid DirectMessageSendRequest request, Principal principal) {
    UUID currentUserId = extractUserId(principal);

    DirectMessageDto saved = directMessageService.send(request, currentUserId);
    log.info("DM 발행 완료: dmId={}", saved.id());

    String destination = StompDestinationUtil.directMessageDestination(
        saved.sender().userId(), saved.receiver().userId());
    messagingTemplate.convertAndSend(destination, saved);
  }

  private UUID extractUserId(Principal principal) {
    UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
    return userPrincipal.userId();
  }

  @MessageExceptionHandler(OtbooException.class)
  @SendToUser("/queue/errors")
  public ErrorResponse handleOtbooException(OtbooException e) {
    log.warn("[{}] {}", e.getClass().getSimpleName(), e.getMessage());
    return new ErrorResponse(e.getClass().getSimpleName(), e.getMessage(), e.getDetails());
  }

  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser("/queue/errors")
  public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
    Map<String, Object> details = e.getBindingResult() == null ? Map.of()
        : e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                (existing, replacement) -> existing
            ));
    log.warn("[{}] {}", e.getClass().getSimpleName(), details);
    return new ErrorResponse(e.getClass().getSimpleName(), "요청 값이 유효하지 않습니다.", details);
  }
}