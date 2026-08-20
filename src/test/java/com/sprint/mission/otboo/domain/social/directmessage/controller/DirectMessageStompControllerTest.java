package com.sprint.mission.otboo.domain.social.directmessage.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.sprint.mission.otboo.domain.social.directmessage.exception.DirectMessageUnauthorizedException;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

    @Test
    @DisplayName("Principal이 null이면 DirectMessageUnauthorizedException을 던진다")
    void Principal이_null이면_DirectMessageUnauthorizedException을_던진다() {
      // given
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .sample();

      // when & then
      assertThatThrownBy(() -> directMessageStompController.send(request, null))
          .isInstanceOf(DirectMessageUnauthorizedException.class);
    }

    @Test
    @DisplayName("Principal 타입이 예상과 다르면 DirectMessageUnauthorizedException을 던진다")
    void Principal_타입이_예상과_다르면_DirectMessageUnauthorizedException을_던진다() {
      // given
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .sample();
      Principal unexpected = () -> "unexpected";

      // when & then
      assertThatThrownBy(() -> directMessageStompController.send(request, unexpected))
          .isInstanceOf(DirectMessageUnauthorizedException.class);
    }
  }
}
