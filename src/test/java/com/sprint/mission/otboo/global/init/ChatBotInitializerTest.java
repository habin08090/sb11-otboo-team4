package com.sprint.mission.otboo.global.init;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.entity.User;
import com.sprint.mission.otboo.domain.authuser.user.entity.enums.Role;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.authuser.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatBotInitializer")
class ChatBotInitializerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private ChatBotInitializer chatBotInitializer(ChatBotProperties properties) {
    return new ChatBotInitializer(userRepository, profileRepository, passwordEncoder, properties);
  }

  private ChatBotProperties defaultChatBotProperties() {
    return new ChatBotProperties("AI 챗봇", "chatbot@test.com", "chatbot-password1");
  }

  @Nested
  @DisplayName("챗봇 계정 초기화 - run")
  class Run {

    @Test
    @DisplayName("이미 동일한 이메일의 챗봇 계정이 존재하면 새로 만들지 않는다")
    void 이미_동일한_이메일의_챗봇_계정이_존재하면_새로_만들지_않는다() throws Exception {
      // given
      ChatBotProperties properties = defaultChatBotProperties();
      ChatBotInitializer initializer = chatBotInitializer(properties);
      given(userRepository.existsByEmail(properties.email())).willReturn(true);

      // when
      initializer.run(null);

      // then
      verify(userRepository, never()).saveAndFlush(any());
      verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("챗봇 계정이 없으면 고정된 ID로 비밀번호를 암호화해서 챗봇 User와 기본 Profile을 생성한다")
    void 챗봇_계정이_없으면_고정된_ID로_비밀번호를_암호화해서_챗봇_User와_기본_Profile을_생성한다()
        throws Exception {
      // given
      ChatBotProperties properties = defaultChatBotProperties();
      ChatBotInitializer initializer = chatBotInitializer(properties);
      given(userRepository.existsByEmail(properties.email())).willReturn(false);
      given(passwordEncoder.encode("chatbot-password1")).willReturn("encoded-chatbot-password");

      // when
      initializer.run(null);

      // then
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).saveAndFlush(userCaptor.capture());
      User savedChatBot = userCaptor.getValue();
      assertThat(savedChatBot.getId()).isEqualTo(ChatBotInitializer.CHAT_BOT_USER_ID);
      assertThat(savedChatBot.getName()).isEqualTo("AI 챗봇");
      assertThat(savedChatBot.getEmail()).isEqualTo("chatbot@test.com");
      assertThat(savedChatBot.getPassword()).isEqualTo("encoded-chatbot-password");
      assertThat(savedChatBot.getRole()).isEqualTo(Role.USER);

      ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
      verify(profileRepository).save(profileCaptor.capture());
      assertThat(profileCaptor.getValue().getUser()).isEqualTo(savedChatBot);
    }

    @Test
    @DisplayName("existsByEmail 통과 후 동시 생성으로 유니크 제약 위반이 나면 예외를 전파하지 않고 Profile도 생성하지 않는다")
    void existsByEmail_통과_후_동시_생성으로_유니크_제약_위반이_나면_예외를_전파하지_않고_Profile도_생성하지_않는다()
        throws Exception {
      // given
      ChatBotProperties properties = defaultChatBotProperties();
      ChatBotInitializer initializer = chatBotInitializer(properties);
      given(userRepository.existsByEmail(properties.email())).willReturn(false);
      given(passwordEncoder.encode(any())).willReturn("encoded-chatbot-password");
      given(userRepository.saveAndFlush(any(User.class)))
          .willThrow(
              new DataIntegrityViolationException("duplicate key value violates unique constraint"));

      // when & then
      assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
      verify(profileRepository, never()).save(any());
    }
  }
}
