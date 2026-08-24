package com.sprint.mission.otboo.domain.social.directmessage.listener;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageBroadcast;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageDto;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.ObjectMapper;

@DisplayName("DirectMessageRedisListener")
class DirectMessageRedisListenerTest {

  private static final String DM_CHANNEL = "dm:messages";
  private static final String DESTINATION = "/sub/direct-messages_"
      + "11111111-1111-1111-1111-111111111111_99999999-9999-9999-9999-999999999999";

  private final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
      .build();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
  private final DirectMessageRedisListener listener =
      new DirectMessageRedisListener(objectMapper, messagingTemplate);

  private DirectMessageBroadcast broadcast() {
    DirectMessageDto dto = fm.giveMeBuilder(DirectMessageDto.class).sample();
    return new DirectMessageBroadcast(DESTINATION, dto);
  }

  private Message messageOf(byte[] body) {
    return new DefaultMessage(DM_CHANNEL.getBytes(StandardCharsets.UTF_8), body);
  }

  @Nested
  @DisplayName("메시지 수신")
  class OnMessage {

    @Test
    @DisplayName("역직렬화한 메시지를 destination으로 전달한다")
    void 역직렬화한_메시지를_destination으로_전달한다() {
      // given
      DirectMessageBroadcast payload = broadcast();
      Message message = messageOf(objectMapper.writeValueAsBytes(payload));

      // when
      listener.onMessage(message, null);

      // then
      verify(messagingTemplate).convertAndSend(payload.destination(), payload.message());
    }

    @Test
    @DisplayName("역직렬화에 실패해도 예외를 전파하지 않고 전달을 호출하지 않는다")
    void 역직렬화에_실패해도_예외를_전파하지_않는다() {
      // given
      Message message = messageOf("not-valid-json".getBytes(StandardCharsets.UTF_8));

      // when & then
      assertThatCode(() -> listener.onMessage(message, null)).doesNotThrowAnyException();
      verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    @DisplayName("전달이 실패해도 예외를 전파하지 않는다")
    void 전달이_실패해도_예외를_전파하지_않는다() {
      // given
      DirectMessageBroadcast payload = broadcast();
      Message message = messageOf(objectMapper.writeValueAsBytes(payload));
      willThrow(new RuntimeException("전달 실패")).given(messagingTemplate)
          .convertAndSend(anyString(), any(Object.class));

      // when & then
      assertThatCode(() -> listener.onMessage(message, null)).doesNotThrowAnyException();
    }
  }
}
