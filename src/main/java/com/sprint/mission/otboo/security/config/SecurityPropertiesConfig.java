package com.sprint.mission.otboo.security.config;

import com.sprint.mission.otboo.global.init.AdminProperties;
import com.sprint.mission.otboo.global.init.ChatBotProperties;
import com.sprint.mission.otboo.global.init.TestUserProperties;
import com.sprint.mission.otboo.security.cookie.properties.RefreshTokenCookieProperties;
import com.sprint.mission.otboo.security.oauth2.properties.OAuth2Properties;
import com.sprint.mission.otboo.security.token.properties.TokenProperties;
import com.sprint.mission.otboo.security.usersession.properties.UserSessionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    TokenProperties.class,
    UserSessionProperties.class,
    RefreshTokenCookieProperties.class,
    AdminProperties.class,
    OAuth2Properties.class,
    TestUserProperties.class,
    ChatBotProperties.class
})
public class SecurityPropertiesConfig {

}
