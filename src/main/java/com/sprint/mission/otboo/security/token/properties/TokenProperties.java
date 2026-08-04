package com.sprint.mission.otboo.security.token.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "otboo.auth.token")
public record TokenProperties(
    @Positive long accessTokenExpirationMinutes,
    @Positive long refreshTokenExpirationDays,
    @NotBlank String accessSecret,
    @NotBlank String refreshSecret
) {

}
