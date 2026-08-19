package com.sprint.mission.otboo.global.config;

import com.sprint.mission.otboo.external.llm.LlmProperties;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableFeignClients(basePackages = "com.sprint.mission.otboo.external")
@EnableConfigurationProperties(LlmProperties.class)
public class FeignConfig {

  @Primary
  @Bean
  public Request.Options feignRequestOptions() {
    return new Request.Options(
        Duration.ofSeconds(3),
        Duration.ofSeconds(10),
        true
    );
  }

  @Primary
  @Bean
  public Retryer feignRetryer() {
    return new Retryer.Default(100, 1000, 3);
  }

  @Bean
  public ErrorDecoder feignErrorDecoder() {
    return new FeignErrorDecoder();
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
