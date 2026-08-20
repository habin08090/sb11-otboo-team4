package com.sprint.mission.otboo.domain.social.directmessage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.authuser.user.exception.UserNotFoundException;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.common.repository.querydsl.UserSummaryQueryRepository;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageParams;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.entity.DirectMessage;
import com.sprint.mission.otboo.domain.social.directmessage.exception.DirectMessageForbiddenException;
import com.sprint.mission.otboo.domain.social.directmessage.exception.DirectMessageUserNotFoundException;
import com.sprint.mission.otboo.domain.social.directmessage.exception.SelfDirectMessageNotAllowedException;
import com.sprint.mission.otboo.domain.social.directmessage.mapper.DirectMessageMapper;
import com.sprint.mission.otboo.domain.social.directmessage.repository.DirectMessageRepository;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import com.sprint.mission.otboo.global.dto.SortDirection;
import com.sprint.mission.otboo.global.event.NotificationRequestedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("DirectMessageService")
class DirectMessageServiceTest {

  static final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
      .plugin(new JakartaValidationPlugin())
      .build();

  @InjectMocks
  DirectMessageService directMessageService;

  @Mock
  DirectMessageRepository directMessageRepository;

  @Mock
  DirectMessageMapper directMessageMapper;

  @Mock
  UserSummaryQueryRepository userSummaryQueryRepository;

  @Mock
  ApplicationEventPublisher eventPublisher;

  @Nested
  @DisplayName("DM 목록 조회")
  class GetDirectMessages {

    @Test
    @DisplayName("Repository가 준 페이지를 DirectMessageDto로 변환해 반환한다")
    void Repository가_준_페이지를_DirectMessageDto로_변환해_반환한다() {
      // given
      UUID me = UUID.randomUUID();
      UUID other = UUID.randomUUID();
      DirectMessageParams params = new DirectMessageParams(other, null, null, 10);

      DirectMessage message = DirectMessage.create(me, other, "안녕하세요?");
      CursorPageResponse<DirectMessage> repoPage = new CursorPageResponse<>(
          List.of(message), null, null, false, 1L, "createdAt", SortDirection.DESCENDING);
      given(directMessageRepository.findDirectMessages(me, params)).willReturn(repoPage);

      UserSummary sender = new UserSummary(me, "나", null);
      UserSummary receiver = new UserSummary(other, "상대", null);
      given(userSummaryQueryRepository.findByUserIds(List.of(me, other)))
          .willReturn(List.of(sender, receiver));

      DirectMessageDto dto = new DirectMessageDto(
          message.getId(), null, sender, receiver, "안녕하세요?");
      given(directMessageMapper.toDto(message, sender, receiver)).willReturn(dto);

      // when
      CursorPageResponse<DirectMessageDto> result =
          directMessageService.getDirectMessages(me, params);

      // then
      assertThat(result.data()).containsExactly(dto);
      assertThat(result.totalCount()).isEqualTo(1L);
      assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("메시지가 없으면 빈 페이지를 반환하고 사용자 배치 조회를 하지 않는다")
    void 메시지가_없으면_빈_페이지를_반환하고_사용자_배치_조회를_하지_않는다() {
      // given
      UUID me = UUID.randomUUID();
      UUID other = UUID.randomUUID();
      DirectMessageParams params = new DirectMessageParams(other, null, null, 10);

      CursorPageResponse<DirectMessage> emptyPage = new CursorPageResponse<>(
          List.of(), null, null, false, 0L, "createdAt", SortDirection.DESCENDING);
      given(directMessageRepository.findDirectMessages(me, params)).willReturn(emptyPage);

      // when
      CursorPageResponse<DirectMessageDto> result =
          directMessageService.getDirectMessages(me, params);

      // then
      assertThat(result.data()).isEmpty();
      assertThat(result.totalCount()).isZero();
      verify(userSummaryQueryRepository, never()).findByUserIds(any());
    }

    @Test
    @DisplayName("사용자 정보를 조회할 수 없으면 DirectMessageUserNotFoundException을 던진다")
    void 사용자_정보를_조회할_수_없으면_DirectMessageUserNotFoundException을_던진다() {
      // given
      UUID me = UUID.randomUUID();
      UUID other = UUID.randomUUID();
      DirectMessageParams params = new DirectMessageParams(other, null, null, 10);

      DirectMessage message = DirectMessage.create(me, other, "안녕하세요?");
      CursorPageResponse<DirectMessage> repoPage = new CursorPageResponse<>(
          List.of(message), null, null, false, 1L, "createdAt", SortDirection.DESCENDING);
      given(directMessageRepository.findDirectMessages(me, params)).willReturn(repoPage);
      given(userSummaryQueryRepository.findByUserIds(any())).willReturn(List.of());

      // when & then
      assertThatThrownBy(() -> directMessageService.getDirectMessages(me, params))
          .isInstanceOf(DirectMessageUserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("DM 전송")
  class Send {

    @Test
    @DisplayName("메시지를 저장하고 DirectMessageDto를 반환한다")
    void 메시지를_저장하고_DirectMessageDto를_반환한다() {
      // given
      UUID senderId = UUID.randomUUID();
      UUID receiverId = UUID.randomUUID();
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .set("senderId", senderId)
          .set("receiverId", receiverId)
          .set("content", "안녕하세요?")
          .sample();

      DirectMessage saved = DirectMessage.create(senderId, receiverId, "안녕하세요?");
      given(directMessageRepository.save(any(DirectMessage.class))).willReturn(saved);

      UserSummary sender = new UserSummary(senderId, "보낸사람", null);
      UserSummary receiver = new UserSummary(receiverId, "받는사람", null);
      given(userSummaryQueryRepository.findByUserId(senderId)).willReturn(sender);
      given(userSummaryQueryRepository.findByUserId(receiverId)).willReturn(receiver);

      DirectMessageDto expected = new DirectMessageDto(
          saved.getId(), null, sender, receiver, "안녕하세요?");
      given(directMessageMapper.toDto(saved, sender, receiver)).willReturn(expected);

      // when
      DirectMessageDto result = directMessageService.send(request, senderId);

      // then
      assertThat(result).isEqualTo(expected);
      ArgumentCaptor<DirectMessage> captor = ArgumentCaptor.forClass(DirectMessage.class);
      verify(directMessageRepository).save(captor.capture());
      assertThat(captor.getValue().getSenderId()).isEqualTo(senderId);
      assertThat(captor.getValue().getReceiverId()).isEqualTo(receiverId);
      assertThat(captor.getValue().getContent()).isEqualTo("안녕하세요?");
    }

    @Test
    @DisplayName("senderId가 인증 사용자와 다르면 DirectMessageForbiddenException을 던진다")
    void senderId가_인증_사용자와_다르면_DirectMessageForbiddenException을_던진다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      UUID otherSenderId = UUID.randomUUID();
      UUID receiverId = UUID.randomUUID();
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .set("senderId", otherSenderId)
          .set("receiverId", receiverId)
          .set("content", "안녕하세요?")
          .sample();

      // when & then
      assertThatThrownBy(() -> directMessageService.send(request, currentUserId))
          .isInstanceOf(DirectMessageForbiddenException.class);
    }

    @Test
    @DisplayName("자기 자신에게 보내면 SelfDirectMessageNotAllowedException을 던진다")
    void 자기_자신에게_보내면_SelfDirectMessageNotAllowedException을_던진다() {
      // given
      UUID userId = UUID.randomUUID();
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .set("senderId", userId)
          .set("receiverId", userId)
          .set("content", "안녕하세요?")
          .sample();

      // when & then
      assertThatThrownBy(() -> directMessageService.send(request, userId))
          .isInstanceOf(SelfDirectMessageNotAllowedException.class);
    }

    @Test
    @DisplayName("전송에 성공하면 수신자에게 알림 이벤트를 발행한다")
    void 전송에_성공하면_수신자에게_알림_이벤트를_발행한다() {
      // given
      UUID senderId = UUID.randomUUID();
      UUID receiverId = UUID.randomUUID();
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .set("senderId", senderId)
          .set("receiverId", receiverId)
          .set("content", "안녕하세요?")
          .sample();

      DirectMessage saved = DirectMessage.create(senderId, receiverId, "안녕하세요?");
      given(directMessageRepository.save(any(DirectMessage.class))).willReturn(saved);
      given(userSummaryQueryRepository.findByUserId(senderId))
          .willReturn(new UserSummary(senderId, "보낸사람", null));
      given(userSummaryQueryRepository.findByUserId(receiverId))
          .willReturn(new UserSummary(receiverId, "받는사람", null));

      // when
      directMessageService.send(request, senderId);

      // then
      ArgumentCaptor<NotificationRequestedEvent> captor =
          ArgumentCaptor.forClass(NotificationRequestedEvent.class);
      verify(eventPublisher).publishEvent(captor.capture());
      NotificationRequestedEvent event = captor.getValue();
      assertThat(event.receiverIds()).containsExactly(receiverId);
      assertThat(event.title()).isEqualTo("[DM] 보낸사람");
      assertThat(event.content()).isEqualTo("안녕하세요?");
    }

    @Test
    @DisplayName("수신자가 존재하지 않으면 저장하지 않고 예외를 던진다")
    void 수신자가_존재하지_않으면_저장하지_않고_예외를_던진다() {
      // given
      UUID senderId = UUID.randomUUID();
      UUID receiverId = UUID.randomUUID();
      DirectMessageSendRequest request = fm.giveMeBuilder(DirectMessageSendRequest.class)
          .set("senderId", senderId)
          .set("receiverId", receiverId)
          .sample();

      UserSummary sender = fm.giveMeBuilder(UserSummary.class)
          .set("userId", senderId)
          .set("name", "발신자")
          .sample();
      given(userSummaryQueryRepository.findByUserId(senderId)).willReturn(sender);
      given(userSummaryQueryRepository.findByUserId(receiverId))
          .willThrow(UserNotFoundException.withNone());

      // when & then
      assertThatThrownBy(() -> directMessageService.send(request, senderId))
          .isInstanceOf(UserNotFoundException.class);
      verify(directMessageRepository, never()).save(any());
    }
  }
}
