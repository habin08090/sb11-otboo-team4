package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.authuser.user.repository.ProfileRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesAttributeRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.repository.WeatherRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RecommendationService {

  private final WeatherRepository weatherRepository;
  private final ProfileRepository profileRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesAttributeRepository clothesAttributeRepository;

  public RecommendationDto recommend(UUID weatherId, UUID userId) {
    throw new UnsupportedOperationException("미구현");
  }
}