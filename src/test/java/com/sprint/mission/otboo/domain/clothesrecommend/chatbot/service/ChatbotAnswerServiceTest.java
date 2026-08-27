package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.otboo.domain.authuser.user.entity.Profile;
import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotAnswerFailedException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.exception.ChatbotRateLimitExceededException;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.mapper.ChatbotWardrobeAssembler;
import com.sprint.mission.otboo.domain.clothesrecommend.chatbot.ratelimit.ChatbotRateLimiter;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.Weather;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.SkyStatus;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.WindStrength;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import com.sprint.mission.otboo.external.llm.LlmChatbotFetcher;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotContext;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import com.sprint.mission.otboo.external.llm.exception.LlmApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatbotAnswerServiceTest {

  private static final String QUESTION = "오늘 뭐 입을까?";

  @InjectMocks
  ChatbotAnswerService chatbotAnswerService;

  @Mock
  ChatbotRateLimiter chatbotRateLimiter;
  @Mock
  WeatherRepository weatherRepository;
  @Mock
  ProfileRepository profileRepository;
  @Mock
  ClothesRepository clothesRepository;
  @Mock
  ChatbotWardrobeAssembler chatbotWardrobeAssembler;
  @Mock
  LlmChatbotFetcher llmChatbotFetcher;

  private static Weather createWeather(double temperature, PrecipitationType precipitationType,
      WindStrength windStrength) {
    Weather weather = Weather.create(
        null, null, null,
        SkyStatus.CLOUDY, precipitationType, 0, 0,
        0, 0.0,
        temperature, 0.0, temperature - 3, temperature + 3,
        windStrength == WindStrength.STRONG ? 15.0 : 3.0,
        windStrength,
        null, null, null, null);
    ReflectionTestUtils.setField(weather, "id", UUID.randomUUID());
    return weather;
  }

  private static Profile createProfile(UUID userId, int temperatureSensitivity) {
    Profile profile = Profile.create(null);
    ReflectionTestUtils.setField(profile, "id", userId);
    ReflectionTestUtils.setField(profile, "temperatureSensitivity", temperatureSensitivity);
    return profile;
  }

  private static Clothes createClothes(UUID ownerId, String name, ClothesType type) {
    Clothes clothes = Clothes.create(ownerId, name, type);
    ReflectionTestUtils.setField(clothes, "id", UUID.randomUUID());
    return clothes;
  }

  private static LlmChatbotWardrobeItem wardrobeItem(String name, ClothesType type) {
    return new LlmChatbotWardrobeItem(name, type, "");
  }

  private LlmChatbotContext captureContext() {
    ArgumentCaptor<LlmChatbotContext> contextCaptor =
        ArgumentCaptor.forClass(LlmChatbotContext.class);
    verify(llmChatbotFetcher).answer(contextCaptor.capture());
    return contextCaptor.getValue();
  }

  @Nested
  @DisplayName("답변 생성")
  class Answer {

    @Test
    @DisplayName("날씨_프로필_옷장을_모아_LLM_답변을_반환한다")
    void 날씨_프로필_옷장을_모아_LLM_답변을_반환한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID weatherId = UUID.randomUUID();
      Weather weather = createWeather(28.0, PrecipitationType.NONE, WindStrength.WEAK);
      Clothes top = createClothes(userId, "리넨 셔츠", ClothesType.TOP);

      given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
      given(profileRepository.findById(userId)).willReturn(Optional.of(createProfile(userId, 4)));
      given(clothesRepository.findActiveByOwnerId(userId)).willReturn(List.of(top));
      given(chatbotWardrobeAssembler.toWardrobeItems(List.of(top)))
          .willReturn(List.of(wardrobeItem("리넨 셔츠", ClothesType.TOP)));
      given(llmChatbotFetcher.answer(any(LlmChatbotContext.class))).willReturn("셔츠를 입어보세요.");

      // when
      String result = chatbotAnswerService.answer(userId, QUESTION, weatherId);

      // then
      assertThat(result).isEqualTo("셔츠를 입어보세요.");

      LlmChatbotContext context = captureContext();
      assertThat(context.question()).isEqualTo(QUESTION);
      assertThat(context.sensitivity()).isEqualTo(4);
      assertThat(context.weather().temperature()).isEqualTo(28.0);
      assertThat(context.weather().precipitationType()).isEqualTo(PrecipitationType.NONE);
      assertThat(context.weather().windStrength()).isEqualTo(WindStrength.WEAK);
      assertThat(context.wardrobe()).hasSize(1);
    }

    @Test
    @DisplayName("weatherId가_없으면_날씨_없이_답변한다")
    void weatherId가_없으면_날씨_없이_답변한다() {
      // given
      UUID userId = UUID.randomUUID();

      given(profileRepository.findById(userId)).willReturn(Optional.of(createProfile(userId, 3)));
      given(clothesRepository.findActiveByOwnerId(userId)).willReturn(List.of());
      given(chatbotWardrobeAssembler.toWardrobeItems(List.of())).willReturn(List.of());
      given(llmChatbotFetcher.answer(any(LlmChatbotContext.class))).willReturn("답변");

      // when
      String result = chatbotAnswerService.answer(userId, QUESTION, null);

      // then
      assertThat(result).isEqualTo("답변");
      assertThat(captureContext().weather()).isNull();
      verifyNoInteractions(weatherRepository);
    }

    @Test
    @DisplayName("weatherId에_해당하는_날씨가_없으면_날씨_없이_답변한다")
    void weatherId에_해당하는_날씨가_없으면_날씨_없이_답변한다() {
      // given
      UUID userId = UUID.randomUUID();
      UUID weatherId = UUID.randomUUID();

      given(weatherRepository.findById(weatherId)).willReturn(Optional.empty());
      given(profileRepository.findById(userId)).willReturn(Optional.of(createProfile(userId, 3)));
      given(clothesRepository.findActiveByOwnerId(userId)).willReturn(List.of());
      given(chatbotWardrobeAssembler.toWardrobeItems(List.of())).willReturn(List.of());
      given(llmChatbotFetcher.answer(any(LlmChatbotContext.class))).willReturn("답변");

      // when
      String result = chatbotAnswerService.answer(userId, QUESTION, weatherId);

      // then
      assertThat(result).isEqualTo("답변");
      assertThat(captureContext().weather()).isNull();
    }

    @Test
    @DisplayName("프로필이_없으면_기본_온도민감도로_답변한다")
    void 프로필이_없으면_기본_온도민감도로_답변한다() {
      // given
      UUID userId = UUID.randomUUID();

      given(profileRepository.findById(userId)).willReturn(Optional.empty());
      given(clothesRepository.findActiveByOwnerId(userId)).willReturn(List.of());
      given(chatbotWardrobeAssembler.toWardrobeItems(List.of())).willReturn(List.of());
      given(llmChatbotFetcher.answer(any(LlmChatbotContext.class))).willReturn("답변");

      // when
      chatbotAnswerService.answer(userId, QUESTION, null);

      // then
      assertThat(captureContext().sensitivity()).isEqualTo(3);
    }

    @Test
    @DisplayName("사용량_제한을_초과하면_LLM을_호출하지_않는다")
    void 사용량_제한을_초과하면_LLM을_호출하지_않는다() {
      // given
      UUID userId = UUID.randomUUID();
      willThrow(ChatbotRateLimitExceededException.withUserId(userId, 5))
          .given(chatbotRateLimiter).check(userId);

      // when & then
      assertThatThrownBy(() -> chatbotAnswerService.answer(userId, QUESTION, null))
          .isInstanceOf(ChatbotRateLimitExceededException.class);
      verifyNoInteractions(llmChatbotFetcher, clothesRepository, profileRepository);
    }

    @Test
    @DisplayName("LLM_호출이_실패하면_ChatbotAnswerFailedException으로_wrap한다")
    void LLM_호출이_실패하면_ChatbotAnswerFailedException으로_wrap한다() {
      // given
      UUID userId = UUID.randomUUID();

      given(profileRepository.findById(userId)).willReturn(Optional.of(createProfile(userId, 3)));
      given(clothesRepository.findActiveByOwnerId(userId)).willReturn(List.of());
      given(chatbotWardrobeAssembler.toWardrobeItems(List.of())).willReturn(List.of());
      given(llmChatbotFetcher.answer(any(LlmChatbotContext.class)))
          .willThrow(LlmApiException.parseFailed());

      // when & then
      assertThatThrownBy(() -> chatbotAnswerService.answer(userId, QUESTION, null))
          .isInstanceOf(ChatbotAnswerFailedException.class);
    }
  }
}
