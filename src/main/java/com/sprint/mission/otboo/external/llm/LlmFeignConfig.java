package com.sprint.mission.otboo.external.llm;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class LlmFeignConfig {

  @Bean
  public Request.Options llmRequestOptions() {
    return new Request.Options(5, TimeUnit.SECONDS, 30, TimeUnit.SECONDS, true);
  }

  @Bean
  public Retryer llmRetryer() {
    return Retryer.NEVER_RETRY;
  }

  @Bean
  public RequestInterceptor llmAuthInterceptor(
      @Value("${external.llm.api-key}") String apiKey) {
    return requestTemplate -> requestTemplate.header("Authorization",
        "Bearer " + apiKey);
  }
}
