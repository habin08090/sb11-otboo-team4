package com.sprint.mission.otboo.domain.social.directmessage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.exception.DirectMessageForbiddenException;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
import com.sprint.mission.otboo.global.dto.ErrorResponse;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectMessageStompController")
class DirectMessageStompControllerTest {

  static final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
      .plugin(new JakartaValidationPlugin())
      .build();

  @InjectMocks
  private DirectMessageStompController directMessageStompController;

  @Mock
  private DirectMessageService directMessageService;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  private Authentication authenticationOf(UUID userId) {
    UserPrincipal principal = new UserPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("USER")));
  }

  @Nested
  @DisplayName("DM 전송")
  class Send {

    @Test
    @DisplayName("메시지를 저장하고 두 사용자의 대화 채널로 발행한다")
    void 메시지를_저장하고_두_사용자의_대화_채널로_발행한다() {
      // given
      UUID senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
      UUID receiverId = UUID.fromString("99999999-9999-9999-9999-999999999999");
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .set("senderId", senderId)
          .set("receiverId", receiverId)
          .set("content", "안녕하세요?")
          .sample();

      DirectMessageDto saved = new DirectMessageDto(
          UUID.randomUUID(), Instant.now(),
          new UserSummary(senderId, "보낸사람", null),
          new UserSummary(receiverId, "받는사람", null),
          "안녕하세요?");
      given(directMessageService.send(any(DirectMessageSendRequest.class), eq(senderId)))
          .willReturn(saved);

      // when
      directMessageStompController.send(request, authenticationOf(senderId));

      // then
      verify(directMessageService).send(request, senderId);
      verify(messagingTemplate).convertAndSend(
          "/sub/direct-messages_11111111-1111-1111-1111-111111111111"
              + "_99999999-9999-9999-9999-999999999999",
          saved);
    }
  }

  @Nested
  @DisplayName("예외 처리")
  class ExceptionHandling {

    @Test
    @DisplayName("OtbooException이 발생하면 예외 정보를 담은 ErrorResponse를 반환한다")
    void OtbooException이_발생하면_예외_정보를_담은_ErrorResponse를_반환한다() {
      // given
      DirectMessageForbiddenException exception =
          DirectMessageForbiddenException.senderMismatch();

      // when
      ErrorResponse result = directMessageStompController.handleOtbooException(exception);

      // then
      assertThat(result.exceptionName()).isEqualTo("DirectMessageForbiddenException");
      assertThat(result.message()).isEqualTo(exception.getMessage());
    }

    @Test
    @DisplayName("검증 실패 시 필드별 메시지를 담은 ErrorResponse를 반환한다")
    void 검증_실패_시_필드별_메시지를_담은_ErrorResponse를_반환한다() throws Exception {
      // given
      DirectMessageSendRequest request =
          new DirectMessageSendRequest(UUID.randomUUID(), UUID.randomUUID(), "");
      BeanPropertyBindingResult bindingResult =
          new BeanPropertyBindingResult(request, "directMessageSendRequest");
      bindingResult.addError(new FieldError(
          "directMessageSendRequest", "content", "공백일 수 없습니다"));

      MethodParameter parameter = new MethodParameter(
          DirectMessageStompController.class.getDeclaredMethod(
              "send", DirectMessageSendRequest.class, Principal.class), 0);
      MethodArgumentNotValidException exception =
          new MethodArgumentNotValidException(
              MessageBuilder.withPayload("").build(), parameter, bindingResult);

      // when
      ErrorResponse result = directMessageStompController.handleValidationException(exception);

      // then
      assertThat(result.exceptionName()).isEqualTo("MethodArgumentNotValidException");
      assertThat(result.message()).isEqualTo("요청 값이 유효하지 않습니다.");
      assertThat(result.details()).containsEntry("content", "공백일 수 없습니다");
    }
  }
}