package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.mapper;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.ClothesAttribute;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesAttributeRepository;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사용자의 의상 목록을 LLM 프롬프트에 실을 형태로 요약한다.
 *
 * <p>속성은 의상마다 따로 조회하지 않고 한 번에 읽어 의상별로 묶는다.
 */
@RequiredArgsConstructor
@Component
public class ChatbotWardrobeAssembler {

  private static final String ATTRIBUTE_DELIMITER = ", ";

  private final ClothesAttributeRepository clothesAttributeRepository;

  public List<LlmChatbotWardrobeItem> toWardrobeItems(List<Clothes> clothesList) {
    if (clothesList.isEmpty()) {
      return List.of();
    }

    List<UUID> clothesIds = clothesList.stream()
        .map(Clothes::getId)
        .toList();
    Map<UUID, List<ClothesAttribute>> attributesByClothesId = clothesAttributeRepository
        .findAllByClothesIdsWithDefinition(clothesIds).stream()
        .collect(Collectors.groupingBy(ClothesAttribute::getClothesId));

    return clothesList.stream()
        .map(clothes -> new LlmChatbotWardrobeItem(
            clothes.getName(),
            clothes.getType(),
            summarize(attributesByClothesId.getOrDefault(clothes.getId(), List.of()))))
        .toList();
  }

  private String summarize(List<ClothesAttribute> attributes) {
    return attributes.stream()
        .map(attribute -> attribute.getDefinition().getName() + "=" + attribute.getValue())
        .collect(Collectors.joining(ATTRIBUTE_DELIMITER));
  }
}
