package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.dto.ChatbotAskRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotAnswerFailedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotUnauthorizedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service.ChatbotAnswerService;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.directmessage.config.DirectMessageRedisConfig;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageBroadcast;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.service.DirectMessageService;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ChatbotStompControllerTest {

  private static final UUID CHATBOT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final String QUESTION = "오늘 뭐 입을까?";

  static FixtureMonkey fixtureMonkey;

  @Mock
  ChatbotAnswerService chatbotAnswerService;
  @Mock
  DirectMessageService directMessageService;
  @Mock
  StringRedisTemplate stringRedisTemplate;

  ObjectMapper objectMapper = new ObjectMapper();

  ChatbotStompController chatbotStompController;

  @BeforeEach
  void setUp() {
    fixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
        .plugin(new JakartaValidationPlugin())
        .build();
    chatbotStompController = new ChatbotStompController(
        chatbotAnswerService, directMessageService, stringRedisTemplate, objectMapper,
        CHATBOT_USER_ID);
  }

  private static Authentication authenticationOf(UUID userId) {
    UserPrincipal principal = new UserPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("USER")));
  }

  private static ChatbotAskRequest askRequest(UUID receiverId, UUID weatherId) {
    return fixtureMonkey.giveMeBuilder(ChatbotAskRequest.class)
        .set("senderId", USER_ID)
        .set("receiverId", receiverId)
        .set("content", QUESTION)
        .set("weatherId", weatherId)
        .sample();
  }

  private static DirectMessageDto dmOf(UUID senderId, UUID receiverId, String content) {
    return new DirectMessageDto(
        UUID.randomUUID(), Instant.now(),
        new UserSummary(senderId, "보낸사람", null),
        new UserSummary(receiverId, "받는사람", null),
        content);
  }

  private void givenDirectMessageEcho() {
    given(directMessageService.send(any(DirectMessageSendRequest.class), any(UUID.class)))
        .willAnswer(invocation -> {
          DirectMessageSendRequest request = invocation.getArgument(0);
          return dmOf(request.senderId(), request.receiverId(), request.content());
        });
  }

  private List<DirectMessageSendRequest> captureSentRequests() {
    ArgumentCaptor<DirectMessageSendRequest> requestCaptor =
        ArgumentCaptor.forClass(DirectMessageSendRequest.class);
    verify(directMessageService, times(2))
        .send(requestCaptor.capture(), any(UUID.class));
    return requestCaptor.getAllValues();
  }

  private List<DirectMessageBroadcast> capturePublished() {
    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(stringRedisTemplate, times(2))
        .convertAndSend(eq(DirectMessageRedisConfig.DM_CHANNEL), payloadCaptor.capture());
    return payloadCaptor.getAllValues().stream()
        .map(payload -> objectMapper.readValue(payload, DirectMessageBroadcast.class))
        .toList();
  }

  @Nested
  @DisplayName("챗봇 질문")
  class Ask {

    @Test
    @DisplayName("질문을_먼저_저장_발행하고_이어서_답변을_저장_발행한다")
    void 질문을_먼저_저장_발행하고_이어서_답변을_저장_발행한다() {
      // given
      UUID weatherId = UUID.randomUUID();
      givenDirectMessageEcho();
      given(chatbotAnswerService.answer(USER_ID, QUESTION, weatherId)).willReturn("셔츠를 입어보세요.");

      // when
      chatbotStompController.ask(askRequest(CHATBOT_USER_ID, weatherId),
          authenticationOf(USER_ID));

      // then
      List<DirectMessageSendRequest> sent = captureSentRequests();
      assertThat(sent.get(0).senderId()).isEqualTo(USER_ID);
      assertThat(sent.get(0).receiverId()).isEqualTo(CHATBOT_USER_ID);
      assertThat(sent.get(0).content()).isEqualTo(QUESTION);
      assertThat(sent.get(1).senderId()).isEqualTo(CHATBOT_USER_ID);
      assertThat(sent.get(1).receiverId()).isEqualTo(USER_ID);
      assertThat(sent.get(1).content()).isEqualTo("셔츠를 입어보세요.");

      List<DirectMessageBroadcast> published = capturePublished();
      String expectedDestination = "/sub/direct-messages_"
          + CHATBOT_USER_ID + "_" + USER_ID;
      assertThat(published.get(0).destination()).isEqualTo(expectedDestination);
      assertThat(published.get(1).destination()).isEqualTo(expectedDestination);
      assertThat(published.get(1).message().content()).isEqualTo("셔츠를 입어보세요.");
    }

    @Test
    @DisplayName("답변을_봇_계정으로_저장한다")
    void 답변을_봇_계정으로_저장한다() {
      // given
      givenDirectMessageEcho();
      given(chatbotAnswerService.answer(eq(USER_ID), eq(QUESTION), any())).willReturn("답변");

      // when
      chatbotStompController.ask(askRequest(CHATBOT_USER_ID, null), authenticationOf(USER_ID));

      // then
      verify(directMessageService).send(any(DirectMessageSendRequest.class), eq(CHATBOT_USER_ID));
    }

    @Test
    @DisplayName("요청의_receiverId가_봇이_아니어도_봇에게_보낸다")
    void 요청의_receiverId가_봇이_아니어도_봇에게_보낸다() {
      // given
      givenDirectMessageEcho();
      given(chatbotAnswerService.answer(eq(USER_ID), eq(QUESTION), any())).willReturn("답변");

      // when
      chatbotStompController.ask(askRequest(UUID.randomUUID(), null), authenticationOf(USER_ID));

      // then
      assertThat(captureSentRequests().get(0).receiverId()).isEqualTo(CHATBOT_USER_ID);
    }

    @Test
    @DisplayName("답변_생성에_실패하면_안내_메시지를_봇_DM으로_보낸다")
    void 답변_생성에_실패하면_안내_메시지를_봇_DM으로_보낸다() {
      // given
      givenDirectMessageEcho();
      ChatbotAnswerFailedException failure =
          ChatbotAnswerFailedException.llmCallFailed(USER_ID, new IllegalStateException("boom"));
      willThrow(failure).given(chatbotAnswerService).answer(eq(USER_ID), eq(QUESTION), any());

      // when
      chatbotStompController.ask(askRequest(CHATBOT_USER_ID, null), authenticationOf(USER_ID));

      // then
      List<DirectMessageSendRequest> sent = captureSentRequests();
      assertThat(sent.get(1).senderId()).isEqualTo(CHATBOT_USER_ID);
      assertThat(sent.get(1).content()).isEqualTo(failure.getMessage());
    }

    @Test
    @DisplayName("사용량_제한을_초과하면_안내_메시지를_봇_DM으로_보낸다")
    void 사용량_제한을_초과하면_안내_메시지를_봇_DM으로_보낸다() {
      // given
      givenDirectMessageEcho();
      ChatbotRateLimitExceededException failure =
          ChatbotRateLimitExceededException.withUserId(USER_ID, 5);
      willThrow(failure).given(chatbotAnswerService).answer(eq(USER_ID), eq(QUESTION), any());

      // when
      chatbotStompController.ask(askRequest(CHATBOT_USER_ID, null), authenticationOf(USER_ID));

      // then
      assertThat(captureSentRequests().get(1).content()).isEqualTo(failure.getMessage());
    }

    @Test
    @DisplayName("Principal이_null이면_ChatbotUnauthorizedException을_던진다")
    void Principal이_null이면_ChatbotUnauthorizedException을_던진다() {
      // given
      ChatbotAskRequest request = askRequest(CHATBOT_USER_ID, null);

      // when & then
      assertThatThrownBy(() -> chatbotStompController.ask(request, null))
          .isInstanceOf(ChatbotUnauthorizedException.class);
    }

    @Test
    @DisplayName("Principal_타입이_예상과_다르면_ChatbotUnauthorizedException을_던진다")
    void Principal_타입이_예상과_다르면_ChatbotUnauthorizedException을_던진다() {
      // given
      ChatbotAskRequest request = askRequest(CHATBOT_USER_ID, null);
      Principal unexpected = () -> "unexpected";

      // when & then
      assertThatThrownBy(() -> chatbotStompController.ask(request, unexpected))
          .isInstanceOf(ChatbotUnauthorizedException.class);
    }
  }
}
