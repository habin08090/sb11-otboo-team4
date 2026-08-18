package com.sprint.mission.otboo.external.llm;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;

// @FeignClient(configuration = ...)로만 로드되는 llmClient 전용 설정.
// @Configuration을 붙이지 않아 컴포넌트 스캔으로 전역에 걸리지 않는다.
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
  public RequestInterceptor llmAuthInterceptor(LlmProperties llmProperties) {
    return requestTemplate -> requestTemplate.header("Authorization",
        "Bearer " + llmProperties.apiKey());
  }
}
