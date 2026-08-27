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
@DisplayName("TestUserInitializer")
class TestUserInitializerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private TestUserInitializer testUserInitializer(TestUserProperties properties) {
    return new TestUserInitializer(userRepository, profileRepository, passwordEncoder, properties);
  }

  private TestUserProperties defaultTestUserProperties() {
    return new TestUserProperties("우디", "woody@test.com", "test-password1");
  }

  @Nested
  @DisplayName("테스트 유저 계정 초기화 - run")
  class Run {

    @Test
    @DisplayName("이미 동일한 이메일의 테스트 유저 계정이 존재하면 새로 만들지 않는다")
    void 이미_동일한_이메일의_테스트_유저_계정이_존재하면_새로_만들지_않는다() throws Exception {
      // given
      TestUserProperties properties = defaultTestUserProperties();
      TestUserInitializer initializer = testUserInitializer(properties);
      given(userRepository.existsByEmail(properties.email())).willReturn(true);

      // when
      initializer.run(null);

      // then
      verify(userRepository, never()).saveAndFlush(any());
      verify(profileRepository, never()).save(any());
    }

    @Test
    @DisplayName("테스트 유저 계정이 없으면 비밀번호를 암호화해서 테스트 유저 User와 기본 Profile을 생성한다")
    void 테스트_유저_계정이_없으면_비밀번호를_암호화해서_테스트_유저_User와_기본_Profile을_생성한다()
        throws Exception {
      // given
      TestUserProperties properties = defaultTestUserProperties();
      TestUserInitializer initializer = testUserInitializer(properties);
      given(userRepository.existsByEmail(properties.email())).willReturn(false);
      given(passwordEncoder.encode("test-password1")).willReturn("encoded-test-password");

      // when
      initializer.run(null);

      // then
      ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
      verify(userRepository).saveAndFlush(userCaptor.capture());
      User savedTestUser = userCaptor.getValue();
      assertThat(savedTestUser.getName()).isEqualTo("우디");
      assertThat(savedTestUser.getEmail()).isEqualTo("woody@test.com");
      assertThat(savedTestUser.getPassword()).isEqualTo("encoded-test-password");
      assertThat(savedTestUser.getRole()).isEqualTo(Role.USER);

      ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
      verify(profileRepository).save(profileCaptor.capture());
      assertThat(profileCaptor.getValue().getUser()).isEqualTo(savedTestUser);
    }

    @Test
    @DisplayName("existsByEmail 통과 후 동시 생성으로 유니크 제약 위반이 나면 예외를 전파하지 않고 Profile도 생성하지 않는다")
    void existsByEmail_통과_후_동시_생성으로_유니크_제약_위반이_나면_예외를_전파하지_않고_Profile도_생성하지_않는다()
        throws Exception {
      // given
      TestUserProperties properties = defaultTestUserProperties();
      TestUserInitializer initializer = testUserInitializer(properties);
      given(userRepository.existsByEmail(properties.email())).willReturn(false);
      given(passwordEncoder.encode(any())).willReturn("encoded-test-password");
      given(userRepository.saveAndFlush(any(User.class)))
          .willThrow(
              new DataIntegrityViolationException("duplicate key value violates unique constraint"));

      // when & then
      assertThatCode(() -> initializer.run(null)).doesNotThrowAnyException();
      verify(profileRepository, never()).save(any());
    }
  }
}
