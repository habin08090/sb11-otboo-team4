package com.sprint.mission.otboo.domain.clothesrecommend.chatbot.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.ClothesAttribute;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.ClothesAttributeRepository;
import com.sprint.mission.otboo.external.llm.dto.LlmChatbotWardrobeItem;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatbotWardrobeAssemblerTest {

  @InjectMocks
  ChatbotWardrobeAssembler chatbotWardrobeAssembler;

  @Mock
  ClothesAttributeRepository clothesAttributeRepository;

  private static Clothes createClothes(UUID ownerId, String name, ClothesType type) {
    Clothes clothes = Clothes.create(ownerId, name, type);
    ReflectionTestUtils.setField(clothes, "id", UUID.randomUUID());
    return clothes;
  }

  private static ClothesAttribute createAttribute(UUID clothesId, String defName, String value) {
    ClothesAttributeDef definition = ClothesAttributeDef.create(defName);
    ReflectionTestUtils.setField(definition, "id", UUID.randomUUID());
    return ClothesAttribute.create(clothesId, definition, value);
  }

  @Nested
  @DisplayName("옷장 요약")
  class ToWardrobeItems {

    @Test
    @DisplayName("의상마다_속성을_이름과_값으로_요약한다")
    void 의상마다_속성을_이름과_값으로_요약한다() {
      // given
      UUID ownerId = UUID.randomUUID();
      Clothes top = createClothes(ownerId, "리넨 셔츠", ClothesType.TOP);

      given(clothesAttributeRepository.findAllByClothesIdsWithDefinition(eq(List.of(top.getId()))))
          .willReturn(List.of(
              createAttribute(top.getId(), "두께감", "얇음"),
              createAttribute(top.getId(), "색상", "흰색")));

      // when
      List<LlmChatbotWardrobeItem> result =
          chatbotWardrobeAssembler.toWardrobeItems(List.of(top));

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).name()).isEqualTo("리넨 셔츠");
      assertThat(result.get(0).type()).isEqualTo(ClothesType.TOP);
      assertThat(result.get(0).attributeSummary()).contains("두께감=얇음").contains("색상=흰색");
    }

    @Test
    @DisplayName("속성이_없는_의상은_요약이_비어_있다")
    void 속성이_없는_의상은_요약이_비어_있다() {
      // given
      UUID ownerId = UUID.randomUUID();
      Clothes shoes = createClothes(ownerId, "운동화", ClothesType.SHOES);

      given(clothesAttributeRepository.findAllByClothesIdsWithDefinition(eq(List.of(shoes.getId()))))
          .willReturn(List.of());

      // when
      List<LlmChatbotWardrobeItem> result =
          chatbotWardrobeAssembler.toWardrobeItems(List.of(shoes));

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).name()).isEqualTo("운동화");
      assertThat(result.get(0).attributeSummary()).isEmpty();
    }

    @Test
    @DisplayName("의상이_없으면_속성을_조회하지_않고_빈_목록을_반환한다")
    void 의상이_없으면_속성을_조회하지_않고_빈_목록을_반환한다() {
      // given
      // when
      List<LlmChatbotWardrobeItem> result = chatbotWardrobeAssembler.toWardrobeItems(List.of());

      // then
      assertThat(result).isEmpty();
      verifyNoInteractions(clothesAttributeRepository);
    }
  }
}
