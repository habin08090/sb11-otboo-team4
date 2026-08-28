package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.dto.ChatbotAskRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotUnauthorizedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service.ChatbotConversationService;
import com.sprint.mission.otboo.security.details.UserPrincipal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class ChatbotStompControllerTest {

  private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  static FixtureMonkey fixtureMonkey;

  @InjectMocks
  ChatbotStompController chatbotStompController;

  @Mock
  ChatbotConversationService chatbotConversationService;

  @BeforeAll
  static void setUpFixtureMonkey() {
    fixtureMonkey = FixtureMonkey.builder()
        .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
        .plugin(new JakartaValidationPlugin())
        .build();
  }

  private static ChatbotAskRequest askRequest() {
    return fixtureMonkey.giveMeBuilder(ChatbotAskRequest.class)
        .set("senderId", USER_ID)
        .set("content", "오늘 뭐 입을까?")
        .sample();
  }

  private static Authentication authenticationOf(UUID userId) {
    UserPrincipal principal = new UserPrincipal(userId, "USER");
    return new UsernamePasswordAuthenticationToken(
        principal, null, List.of(new SimpleGrantedAuthority("USER")));
  }

  @Nested
  @DisplayName("챗봇 질문")
  class Ask {

    @Test
    @DisplayName("인증된_사용자_ID와_요청을_대화_서비스에_넘긴다")
    void 인증된_사용자_ID와_요청을_대화_서비스에_넘긴다() {
      // given
      ChatbotAskRequest request = askRequest();

      // when
      chatbotStompController.ask(request, authenticationOf(USER_ID));

      // then
      verify(chatbotConversationService).ask(USER_ID, request);
    }

    @Test
    @DisplayName("Principal이_null이면_ChatbotUnauthorizedException을_던진다")
    void Principal이_null이면_ChatbotUnauthorizedException을_던진다() {
      // given
      ChatbotAskRequest request = askRequest();

      // when & then
      assertThatThrownBy(() -> chatbotStompController.ask(request, null))
          .isInstanceOf(ChatbotUnauthorizedException.class);
      verifyNoInteractions(chatbotConversationService);
    }

    @Test
    @DisplayName("Principal_타입이_예상과_다르면_ChatbotUnauthorizedException을_던진다")
    void Principal_타입이_예상과_다르면_ChatbotUnauthorizedException을_던진다() {
      // given
      ChatbotAskRequest request = askRequest();
      Principal unexpected = () -> "unexpected";

      // when & then
      assertThatThrownBy(() -> chatbotStompController.ask(request, unexpected))
          .isInstanceOf(ChatbotUnauthorizedException.class);
      verifyNoInteractions(chatbotConversationService);
    }
  }
}
