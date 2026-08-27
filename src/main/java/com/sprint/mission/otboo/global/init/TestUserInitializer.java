package com.sprint.mission.otboo.global.init;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.entity.User;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.authuser.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestUserInitializer implements ApplicationRunner {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final TestUserProperties testUserProperties;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (userRepository.existsByEmail(testUserProperties.email())) {
      log.info("테스트 유저 계정이 이미 존재합니다: {}", testUserProperties.email());
      return;
    }

    User testUser = User.create(
        testUserProperties.name(),
        testUserProperties.email(),
        passwordEncoder.encode(testUserProperties.password())
    );
    try {
      userRepository.saveAndFlush(testUser);
    } catch (DataIntegrityViolationException e) {
      log.info("테스트 유저 계정이 이미 존재합니다: {}", testUserProperties.email());
      return;
    }

    Profile testUserProfile = Profile.create(testUser);
    profileRepository.save(testUserProfile);

    log.info("테스트 유저 계정이 생성되었습니다: {}", testUserProperties.email());
  }
}
