package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import java.util.UUID;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RecommendationService {

  public RecommendationDto recommend(UUID weatherId, UUID userId) {
    throw new UnsupportedOperationException("미구현");
  }
}