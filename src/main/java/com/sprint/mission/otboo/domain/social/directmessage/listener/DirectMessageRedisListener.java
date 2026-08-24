package com.sprint.mission.otboo.domain.social.directmessage.listener;

import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageBroadcast;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Component
public class DirectMessageRedisListener implements MessageListener {

  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
    try {
      DirectMessageBroadcast broadcast =
          objectMapper.readValue(message.getBody(), DirectMessageBroadcast.class);
      messagingTemplate.convertAndSend(broadcast.destination(), broadcast.message());
    } catch (Exception e) {
      log.error("DM Pub/Sub 메시지 처리 실패: channel={}", channel, e);
    }
  }
}
