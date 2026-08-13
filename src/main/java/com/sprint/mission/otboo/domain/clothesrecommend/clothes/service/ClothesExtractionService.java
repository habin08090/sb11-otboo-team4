package com.sprint.mission.otboo.domain.clothesrecommend.clothes.service;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.external.llm.LlmClient;
import com.sprint.mission.otboo.external.purchase.PurchasePageClient;
import com.sprint.mission.otboo.external.purchase.PurchasePageParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class ClothesExtractionService {

  private final PurchasePageClient purchasePageClient;
  private final PurchasePageParser purchasePageParser;
  private final LlmClient llmClient;

  @Value("${external.llm.api-key}")
  private String llmApiKey;

  public ClothesDto extractByUrl(String url) {
    throw new UnsupportedOperationException("미구현");
  }
}
