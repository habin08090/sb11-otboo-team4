package com.sprint.mission.otboo.global.config;

import com.sprint.mission.otboo.security.interceptor.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws").withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes(StompDestinations.APPLICATION_PREFIX);
    // SimpleBroker는 구독 정보를 이 JVM 메모리에만 보관한다. 스케일아웃 전에 외부 브로커 전환 필요
    registry.enableSimpleBroker(StompDestinations.BROKER_PREFIX);
    registry.setUserDestinationPrefix(StompDestinations.USER_PREFIX);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompAuthChannelInterceptor);
  }
}
