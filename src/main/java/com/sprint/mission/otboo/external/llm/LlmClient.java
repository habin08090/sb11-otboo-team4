package com.sprint.mission.otboo.external.llm;

import com.sprint.mission.otboo.external.llm.dto.LlmChatRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmChatResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "llmClient", url = "${external.llm.base-url:https://openrouter.ai/api/v1}", configuration = LlmFeignConfig.class)
public interface LlmClient {

  @PostMapping("/chat/completions")
  LlmChatResponse chat(@RequestBody LlmChatRequest request);
}
