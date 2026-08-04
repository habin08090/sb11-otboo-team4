package com.sprint.mission.otboo.security.usersession.config;

import com.sprint.mission.otboo.security.usersession.registry.UserSessionRegistry;
import com.sprint.mission.otboo.security.usersession.registry.impl.UserSessionRedisRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class UserSessionRegistryConfig {

  @Bean
  @ConditionalOnProperty(name = "otboo.auth.user-session.impl", havingValue = "redis", matchIfMissing = true)
  public UserSessionRegistry userSessionRedisRegistry(StringRedisTemplate redisTemplate) {
    return new UserSessionRedisRegistry(redisTemplate);
  }
}
