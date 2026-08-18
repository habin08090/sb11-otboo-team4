package com.sprint.mission.otboo.external.llm;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "external.llm")
@Validated
public record LlmProperties(
    @NotBlank String apiKey,
    @NotBlank String model
) {

}
