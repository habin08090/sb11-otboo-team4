package com.sprint.mission.otboo.domain.social.directmessage.config;

import com.sprint.mission.otboo.domain.social.directmessage.listener.DirectMessageRedisListener;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class DirectMessageRedisConfig {

  public static final String DM_CHANNEL = "dm:messages";

  @Bean
  public RedisMessageListenerContainer dmMessageListenerContainer(
      RedisConnectionFactory connectionFactory, DirectMessageRedisListener listener,
      @Qualifier("dmListenerExecutor") Executor dmListenerExecutor) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listener, new ChannelTopic(DM_CHANNEL));
    container.setTaskExecutor(dmListenerExecutor);
    return container;
  }
}
