package com.sprint.mission.otboo.security.config;

import com.sprint.mission.otboo.global.init.AdminProperties;
import com.sprint.mission.otboo.security.cookie.RefreshCookieProperties;
import com.sprint.mission.otboo.security.token.properties.TokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    TokenProperties.class,
    RefreshCookieProperties.class,
    AdminProperties.class
})
public class SecurityPropertiesConfig {

}
