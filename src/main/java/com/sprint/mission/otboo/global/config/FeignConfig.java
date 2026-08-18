package com.sprint.mission.otboo.global.config;

import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.time.Duration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.sprint.mission.otboo.external.llm.LlmProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableFeignClients(basePackages = "com.sprint.mission.otboo.external")
@EnableConfigurationProperties(LlmProperties.class)
public class FeignConfig {

  @Bean
  public Request.Options feignRequestOptions() {
    return new Request.Options(
        Duration.ofSeconds(3),
        Duration.ofSeconds(10),
        true
    );
  }

  @Bean
  public Retryer feignRetryer() {
    return new Retryer.Default(100, 1000, 3);
  }

  @Bean
  public ErrorDecoder feignErrorDecoder() {
    return new FeignErrorDecoder();
  }
}
