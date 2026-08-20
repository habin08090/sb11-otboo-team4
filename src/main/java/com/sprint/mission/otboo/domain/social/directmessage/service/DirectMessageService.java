package com.sprint.mission.otboo.domain.social.directmessage.service;

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
import com.sprint.mission.otboo.global.event.NotificationLevel;
import com.sprint.mission.otboo.global.event.NotificationRequestedEvent;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class DirectMessageService {

  private final DirectMessageRepository directMessageRepository;
  private final DirectMessageMapper directMessageMapper;
  private final UserSummaryQueryRepository userSummaryQueryRepository;
  private final ApplicationEventPublisher eventPublisher;

  public CursorPageResponse<DirectMessageDto> getDirectMessages(UUID currentUserId,
      DirectMessageParams params) {
    return toDtoPage(directMessageRepository.findDirectMessages(currentUserId, params));
  }

  @Transactional
  public DirectMessageDto send(DirectMessageSendRequest request, UUID currentUserId) {
    validateSendRequest(request, currentUserId);

    UserSummary sender = userSummaryQueryRepository.findByUserId(request.senderId());
    UserSummary receiver = userSummaryQueryRepository.findByUserId(request.receiverId());

    DirectMessage message = directMessageRepository.save(
        DirectMessage.create(request.senderId(), request.receiverId(), request.content()));
    log.info("DM 전송 완료: dmId={}", message.getId());

    publishDirectMessageNotification(message.getReceiverId(), sender.name(), message.getContent());

    return directMessageMapper.toDto(message, sender, receiver);
  }

  private CursorPageResponse<DirectMessageDto> toDtoPage(CursorPageResponse<DirectMessage> page) {
    List<DirectMessage> messages = page.data();

    Map<UUID, UserSummary> summaryMap = messages.isEmpty() ? Map.of() :
        userSummaryQueryRepository.findByUserIds(
                messages.stream()
                    .flatMap(m -> Stream.of(m.getSenderId(), m.getReceiverId()))
                    .distinct().toList()
            ).stream()
            .collect(Collectors.toMap(UserSummary::userId, Function.identity()));

    List<DirectMessageDto> data = messages.stream()
        .map(m -> {
          UserSummary sender = summaryMap.get(m.getSenderId());
          UserSummary receiver = summaryMap.get(m.getReceiverId());
          if (sender == null || receiver == null) {
            log.warn("DM 사용자 정보를 조회할 수 없습니다: dmId={}", m.getId());
            throw DirectMessageUserNotFoundException.withNone();
          }
          return directMessageMapper.toDto(m, sender, receiver);
        })
        .toList();

    return new CursorPageResponse<>(data, page.nextCursor(), page.nextIdAfter(),
        page.hasNext(), page.totalCount(), page.sortBy(), page.sortDirection());
  }

  private void validateSendRequest(DirectMessageSendRequest request, UUID currentUserId) {
    validateSenderMatchesCurrentUser(request.senderId(), currentUserId);
    validateNotSelfMessage(request.senderId(), request.receiverId());
  }

  private void validateSenderMatchesCurrentUser(UUID senderId, UUID currentUserId) {
    if (!senderId.equals(currentUserId)) {
      throw DirectMessageForbiddenException.senderMismatch();
    }
  }

  private void validateNotSelfMessage(UUID senderId, UUID receiverId) {
    if (senderId.equals(receiverId)) {
      throw SelfDirectMessageNotAllowedException.withNone();
    }
  }

  private void publishDirectMessageNotification(UUID receiverId, String senderName,
      String content) {
    String title = "[DM] " + senderName;
    eventPublisher.publishEvent(
        new NotificationRequestedEvent(Set.of(receiverId), title, content,
            NotificationLevel.INFO));
  }
}
