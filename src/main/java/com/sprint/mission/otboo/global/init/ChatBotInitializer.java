package com.sprint.mission.otboo.global.init;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.entity.User;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.authuser.user.repository.UserRepository;
import java.util.UUID;
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
public class ChatBotInitializer implements ApplicationRunner {

  public static final UUID CHAT_BOT_USER_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;
  private final ChatBotProperties chatBotProperties;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (userRepository.existsByEmail(chatBotProperties.email())) {
      log.info("챗봇 계정이 이미 존재합니다: {}", chatBotProperties.email());
      return;
    }

    User chatBot = User.createChatBot(
        CHAT_BOT_USER_ID,
        chatBotProperties.name(),
        chatBotProperties.email(),
        passwordEncoder.encode(chatBotProperties.password())
    );
    try {
      userRepository.saveAndFlush(chatBot);
    } catch (DataIntegrityViolationException e) {
      log.info("챗봇 계정이 이미 존재합니다: {}", chatBotProperties.email());
      return;
    }

    Profile chatBotProfile = Profile.create(chatBot);
    profileRepository.save(chatBotProfile);

    log.info("챗봇 계정이 생성되었습니다: {}", chatBotProperties.email());
  }
}
