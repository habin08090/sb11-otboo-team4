package com.sprint.mission.otboo.domain.social.follow;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.authuser.user.entity.User;
import com.sprint.mission.otboo.domain.authuser.user.repository.UserRepository;
import com.sprint.mission.otboo.domain.social.follow.dto.FollowCreateRequest;
import com.sprint.mission.otboo.domain.social.follow.dto.FollowDto;
import com.sprint.mission.otboo.domain.social.follow.repository.FollowRepository;
import com.sprint.mission.otboo.domain.social.follow.service.FollowService;
import com.sprint.mission.otboo.global.testcontainers.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Follow 통합 테스트")
class FollowIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private FollowService followService;

  @Autowired
  private FollowRepository followRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager em;

  private User persistUser(String name) {
    return userRepository.save(
        User.create(name, UUID.randomUUID() + "@otboo.io", "password"));
  }

  @Nested
  @DisplayName("팔로우 생성")
  class CreateFollow {

    @Test
    @DisplayName("팔로우를 생성하면 DB에 저장되고 FollowDto를 반환한다")
    void 팔로우를_생성하면_DB에_저장되고_FollowDto를_반환한다() {
      // given
      User follower = persistUser("팔로워");
      User followee = persistUser("팔로위");
      em.flush();

      FollowCreateRequest request =
          new FollowCreateRequest(followee.getId(), follower.getId());

      // when
      FollowDto result = followService.create(request, follower.getId());

      em.flush();
      em.clear();

      // then
      assertThat(result.id()).isNotNull();
      assertThat(result.follower().userId()).isEqualTo(follower.getId());
      assertThat(result.followee().userId()).isEqualTo(followee.getId());

      assertThat(followRepository.findByFollowerIdAndFolloweeId(
          follower.getId(), followee.getId())).isPresent();
    }

    @Test
    @DisplayName("이미 팔로우 중이면 새로 저장하지 않고 기존 관계를 반환한다")
    void 이미_팔로우_중이면_새로_저장하지_않고_기존_관계를_반환한다() {
      // given
      User follower = persistUser("팔로워");
      User followee = persistUser("팔로위");
      em.flush();

      FollowCreateRequest request =
          new FollowCreateRequest(followee.getId(), follower.getId());
      FollowDto first = followService.create(request, follower.getId());
      em.flush();
      em.clear();

      // when
      FollowDto second = followService.create(request, follower.getId());
      em.flush();
      em.clear();

      // then
      assertThat(second.id()).isEqualTo(first.id());
      assertThat(followRepository.count()).isEqualTo(1);
    }
  }
}
