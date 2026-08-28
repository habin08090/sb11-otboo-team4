package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.mapper.ChatbotWardrobeAssembler;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWeather;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LLM에 넘길 컨텍스트를 DB에서 모은다.
 *
 * <p>LLM 호출과 분리해 둔 이유는 트랜잭션 범위 때문이다. 답변 생성은 2~4초 걸리는데, 그 구간까지 읽기 트랜잭션에 들어가 있으면 응답을 기다리는 동안 DB 커넥션을
 * 붙잡고 있게 된다. 조회는 여기서 끝내고 커넥션을 반납한 뒤 LLM을 호출한다.
 *
 * <p>날씨는 화면이 이미 조회해 둔 {@code weatherId}로 받는다. 챗봇이 좌표로 날씨를 다시 조회하면 기상청 API까지 갈 수 있어, LLM 응답 시간에 외부 호출이
 * 하나 더 얹힌다. 값이 없거나 만료돼 조회되지 않으면 날씨 없이 답한다 — 사용자가 질문에 직접 날씨를 적는 경우가 많다.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ChatbotContextProvider {

  private static final int DEFAULT_SENSITIVITY = 3;

  private final WeatherRepository weatherRepository;
  private final ProfileRepository profileRepository;
  private final ClothesRepository clothesRepository;
  private final ChatbotWardrobeAssembler chatbotWardrobeAssembler;

  public LlmChatbotContext collect(UUID userId, String question, UUID weatherId) {
    LlmChatbotWeather weather = findWeather(weatherId);
    int sensitivity = profileRepository.findById(userId)
        .map(Profile::getTemperatureSensitivity)
        .orElse(DEFAULT_SENSITIVITY);

    List<Clothes> clothesList = clothesRepository.findActiveByOwnerId(userId);
    List<LlmChatbotWardrobeItem> wardrobe = chatbotWardrobeAssembler.toWardrobeItems(clothesList);

    return new LlmChatbotContext(question, weather, sensitivity, wardrobe);
  }

  private LlmChatbotWeather findWeather(UUID weatherId) {
    if (weatherId == null) {
      return null;
    }
    return weatherRepository.findById(weatherId)
        .map(this::toChatbotWeather)
        .orElseGet(() -> {
          log.warn("챗봇 질문에 실린 날씨를 찾지 못해 날씨 없이 답한다 weatherId={}", weatherId);
          return null;
        });
  }

  private LlmChatbotWeather toChatbotWeather(Weather weather) {
    return new LlmChatbotWeather(
        weather.getTemperatureCurrent(), weather.getPrecipitationType(), weather.getWindAsWord());
  }
}
