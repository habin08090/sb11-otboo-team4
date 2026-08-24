package com.sprint.mission.otboo.domain.social.common.repository.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.entity.User;
import com.sprint.mission.otboo.domain.authuser.user.exception.UserNotFoundException;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.common.repository.querydsl.impl.UserSummaryQueryRepositoryImpl;
import com.sprint.mission.otboo.global.config.JpaConfig;
import com.sprint.mission.otboo.global.config.QuerydslConfig;
import com.sprint.mission.otboo.global.file.properties.FileImplType;
import com.sprint.mission.otboo.global.file.properties.FileProperties;
import com.sprint.mission.otboo.global.file.util.FileUrlResolver;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class, UserSummaryQueryRepositoryImpl.class,
    UserSummaryQueryRepositoryImplTest.FileUrlResolverTestConfig.class})
@DisplayName("UserSummaryQueryRepository")
class UserSummaryQueryRepositoryImplTest {

  @Autowired
  private UserSummaryQueryRepository userSummaryQueryRepository;

  @Autowired
  private TestEntityManager testEntityManager;

  @TestConfiguration
  static class FileUrlResolverTestConfig {

    // FileProperties가 @ConfigurationProperties라 슬라이스 테스트에서 바인딩되지 않아 직접 생성한다.
    @Bean
    FileUrlResolver fileUrlResolver() {
      return new FileUrlResolver(new FileProperties(
          FileImplType.LOCAL, "http://localhost:8080/uploads", 5242880L, Set.of("jpg"),
          new FileProperties.Local("uploads"), null));
    }
  }

  @Nested
  @DisplayName("findByUserId")
  class FindByUserId {

    @Test
    @DisplayName("유저 ID로 name과 profileImageUrl을 채운 UserSummary를 반환한다")
    void 유저_ID로_name과_profileImageUrl을_채운_UserSummary를_반환한다() {
      // given
      User user = User.create("otboo", "otboo@test.com", "encoded-password");
      testEntityManager.persist(user);
      Profile profile = Profile.create(user);
      testEntityManager.persist(profile);
      testEntityManager.flush();
      testEntityManager.clear();

      // when
      UserSummary result = userSummaryQueryRepository.findByUserId(user.getId());

      // then
      assertThat(result).isNotNull();
      assertThat(result.userId()).isEqualTo(user.getId());
      assertThat(result.name()).isEqualTo("otboo");
      assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("프로필 이미지가 있으면 profileImageUrl까지 채운 UserSummary를 반환한다")
    void 프로필_이미지가_있으면_profileImageUrl까지_채운_UserSummary를_반환한다() {
      // given
      User user = User.create("otboo", "otboo@test.com", "encoded-password");
      testEntityManager.persist(user);
      Profile profile = Profile.create(user);
      ReflectionTestUtils.setField(profile, "profileImageUrl", "profile/otboo.png");
      testEntityManager.persist(profile);
      testEntityManager.flush();
      testEntityManager.clear();

      // when
      UserSummary result = userSummaryQueryRepository.findByUserId(user.getId());

      // then
      assertThat(result.userId()).isEqualTo(user.getId());
      assertThat(result.name()).isEqualTo("otboo");
      assertThat(result.profileImageUrl())
          .isEqualTo("http://localhost:8080/uploads/profile/otboo.png");
    }

    @Test
    @DisplayName("존재하지 않는 userId면 UserNotFoundException을 던진다")
    void 존재하지_않는_userId면_UserNotFoundException을_던진다() {
      // given
      UUID unknownId = UUID.randomUUID();

      // when & then
      assertThatThrownBy(() -> userSummaryQueryRepository.findByUserId(unknownId))
          .isInstanceOf(UserNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("existsByUserId")
  class ExistsByUserId {

    @Test
    @DisplayName("존재하는 userId면 true를 반환한다")
    void 존재하는_userId면_true를_반환한다() {
      // given
      User user = User.create("otboo", "otboo@test.com", "encoded-password");
      testEntityManager.persist(user);
      testEntityManager.flush();
      testEntityManager.clear();

      // when
      boolean result = userSummaryQueryRepository.existsByUserId(user.getId());

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 userId면 false를 반환한다")
    void 존재하지_않는_userId면_false를_반환한다() {
      // given
      UUID unknownId = UUID.randomUUID();

      // when
      boolean result = userSummaryQueryRepository.existsByUserId(unknownId);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("findByUserIds")
  class FindByUserIds {

    @Test
    @DisplayName("여러 userId로 조회하면 해당 UserSummary들을 반환한다")
    void 여러_userId로_조회하면_해당_UserSummary들을_반환한다() {
      // given
      User user1 = testEntityManager.persist(User.create("우디", "woody@otboo.io", "password"));
      User user2 = testEntityManager.persist(User.create("버즈", "buzz@otboo.io", "password"));
      testEntityManager.persist(Profile.create(user1));
      testEntityManager.persist(Profile.create(user2));
      testEntityManager.flush();
      testEntityManager.clear();

      // when
      List<UserSummary> result = userSummaryQueryRepository.findByUserIds(
          List.of(user1.getId(), user2.getId()));

      // then
      assertThat(result)
          .extracting(UserSummary::userId)
          .containsExactlyInAnyOrder(user1.getId(), user2.getId());
    }

    @Test
    @DisplayName("프로필 이미지가 있으면 완전한 URL로 변환해 반환한다")
    void 프로필_이미지가_있으면_완전한_URL로_변환해_반환한다() {
      // given
      User user = testEntityManager.persist(User.create("우디", "woody@otboo.io", "password"));
      Profile profile = Profile.create(user);
      ReflectionTestUtils.setField(profile, "profileImageUrl", "profile/woody.png");
      testEntityManager.persist(profile);
      testEntityManager.flush();
      testEntityManager.clear();

      // when
      List<UserSummary> result = userSummaryQueryRepository.findByUserIds(List.of(user.getId()));

      // then
      assertThat(result)
          .singleElement()
          .extracting(UserSummary::profileImageUrl)
          .isEqualTo("http://localhost:8080/uploads/profile/woody.png");
    }
  }
}
