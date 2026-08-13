package com.sprint.mission.otboo.external.llm;

import com.sprint.mission.otboo.external.llm.dto.LlmExtractionRequest;
import com.sprint.mission.otboo.external.llm.dto.LlmExtractionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "llmClient", url = "${external.llm.base-url}")
public interface LlmClient {

  @PostMapping("/chat/completions")
  LlmExtractionResponse extract(
      @RequestHeader("Authorization") String authorization,
      @RequestBody LlmExtractionRequest request
  );
}
