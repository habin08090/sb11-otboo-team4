package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDefValue;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClothesAttributeDefMapperTest")
class ClothesAttributeDefMapperTest {

  private final ClothesAttributeDefMapper mapper = new ClothesAttributeDefMapper();

  @Test
  @DisplayName("Entity와 Value 목록을 DTO로 변환한다")
  void converts_entity_and_values_to_dto() {
    // given
    ClothesAttributeDef definition = ClothesAttributeDef.create("색상");
    List<ClothesAttributeDefValue> values = List.of(
        ClothesAttributeDefValue.create(definition, "빨강", 0),
        ClothesAttributeDefValue.create(definition, "파랑", 1));

    // when
    ClothesAttributeDefDto dto = mapper.toDto(definition, values);

    // then
    assertThat(dto.name()).isEqualTo("색상");
    assertThat(dto.id()).isEqualTo(definition.getId());
    assertThat(dto.selectableValues()).containsExactly("빨강", "파랑");
  }

  @Test
  @DisplayName("값이 없으면 빈 목록으로 변환한다")
  void converts_with_empty_values() {
    // given
    ClothesAttributeDef definition = ClothesAttributeDef.create("두께감");

    // when
    ClothesAttributeDefDto dto = mapper.toDto(definition, List.of());

    // then
    assertThat(dto.name()).isEqualTo("두께감");
    assertThat(dto.selectableValues()).isEmpty();
  }

  @Test
  @DisplayName("sortOrder 순서대로 정렬된다")
  void sorts_values_by_sort_order() {
    // given
    ClothesAttributeDef definition = ClothesAttributeDef.create("색상");
    List<ClothesAttributeDefValue> values = List.of(
        ClothesAttributeDefValue.create(definition, "파랑", 2),
        ClothesAttributeDefValue.create(definition, "빨강", 0),
        ClothesAttributeDefValue.create(definition, "초록", 1));

    // when
    ClothesAttributeDefDto dto = mapper.toDto(definition, values);

    // then
    assertThat(dto.selectableValues()).containsExactly("빨강", "초록", "파랑");
  }
}