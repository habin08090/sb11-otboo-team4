package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDefValue;
import com.sprint.mission.otboo.global.config.JpaConfig;
import com.sprint.mission.otboo.global.config.QuerydslConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
@DisplayName("ClothesAttributeDefValueRepository")
class ClothesAttributeDefValueRepositoryTest {

  @Autowired
  private ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Autowired
  private ClothesAttributeDefValueRepository clothesAttributeDefValueRepository;

  private ClothesAttributeDef saveDefinition(String name) {
    return clothesAttributeDefRepository.save(ClothesAttributeDef.create(name));
  }

  private ClothesAttributeDefValue saveValue(
      ClothesAttributeDef definition, String value, int sortOrder) {
    return clothesAttributeDefValueRepository.save(
        ClothesAttributeDefValue.create(definition, value, sortOrder));
  }

  @Nested
  @DisplayName("FindAllByDefinitionIds")
  class FindAllByDefinitionIds {

    @Test
    @DisplayName("정의 ID 목록에 해당하는 값들을 sortOrder 순으로 조회한다")
    void 정의_ID_목록에_해당하는_값들을_sortOrder_순으로_조회한다() {
      // given
      ClothesAttributeDef colorDef = saveDefinition("컬러");
      saveValue(colorDef, "빨강", 0);
      saveValue(colorDef, "파랑", 1);
      saveValue(colorDef, "초록", 2);

      // when
      List<ClothesAttributeDefValue> result =
          clothesAttributeDefValueRepository.findAllByDefinitionIds(
              List.of(colorDef.getId()));

      // then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getValue()).isEqualTo("빨강");
      assertThat(result.get(1).getValue()).isEqualTo("파랑");
      assertThat(result.get(2).getValue()).isEqualTo("초록");
    }

    @Test
    @DisplayName("여러 정의의 값들을 한 번에 조회한다")
    void 여러_정의의_값들을_한_번에_조회한다() {
      // given
      ClothesAttributeDef colorDef = saveDefinition("컬러");
      ClothesAttributeDef sizeDef = saveDefinition("사이즈");
      saveValue(colorDef, "빨강", 0);
      saveValue(sizeDef, "S", 0);
      saveValue(sizeDef, "M", 1);

      // when
      List<ClothesAttributeDefValue> result =
          clothesAttributeDefValueRepository.findAllByDefinitionIds(
              List.of(colorDef.getId(), sizeDef.getId()));

      // then
      assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("해당하는 정의가 없으면 빈 리스트를 반환한다")
    void 해당하는_정의가_없으면_빈_리스트를_반환한다() {
      // when
      List<ClothesAttributeDefValue> result =
          clothesAttributeDefValueRepository.findAllByDefinitionIds(
              List.of(UUID.randomUUID()));

      // then
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("DeleteAllByDefinitionId")
  class DeleteAllByDefinitionId {

    @Test
    @DisplayName("해당 정의의 값들만 벌크 삭제한다")
    void 해당_정의의_값들만_벌크_삭제한다() {
      // given
      ClothesAttributeDef colorDef = saveDefinition("컬러");
      ClothesAttributeDef sizeDef = saveDefinition("사이즈");
      saveValue(colorDef, "빨강", 0);
      saveValue(colorDef, "파랑", 1);
      saveValue(sizeDef, "S", 0);

      // when
      clothesAttributeDefValueRepository.deleteAllByDefinitionId(colorDef.getId());

      // then
      List<ClothesAttributeDefValue> remaining =
          clothesAttributeDefValueRepository.findAllByDefinitionIds(
              List.of(colorDef.getId(), sizeDef.getId()));
      assertThat(remaining).hasSize(1);
      assertThat(remaining.get(0).getValue()).isEqualTo("S");
    }

    @Test
    @DisplayName("해당 정의의 값이 없어도 예외가 발생하지 않는다")
    void 해당_정의의_값이_없어도_예외가_발생하지_않는다() {
      // when & then
      clothesAttributeDefValueRepository.deleteAllByDefinitionId(UUID.randomUUID());
    }
  }
}