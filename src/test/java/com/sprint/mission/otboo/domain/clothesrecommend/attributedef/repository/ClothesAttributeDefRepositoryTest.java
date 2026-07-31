package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import com.sprint.mission.otboo.global.config.JpaConfig;
import com.sprint.mission.otboo.global.config.QuerydslConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
@DisplayName("ClothesAttributeDefRepository")
class ClothesAttributeDefRepositoryTest {

  @Autowired
  private ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Nested
  @DisplayName("ExistsByName")
  class ExistsByName {

    @Test
    @DisplayName("존재하는 이름이면 true를 반환한다")
    void 존재하는_이름이면_true를_반환한다() {
      // given
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("컬러"));

      // when
      boolean exists = clothesAttributeDefRepository.existsByName("컬러");

      // then
      assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이름이면 false를 반환한다")
    void 존재하지_않는_이름이면_false를_반환한다() {
      // when
      boolean exists = clothesAttributeDefRepository.existsByName("컬러");

      // then
      assertThat(exists).isFalse();
    }
  }

  @Nested
  @DisplayName("FindAllByNameContainingIgnoreCase")
  class FindAllByNameContainingIgnoreCase {

    @Test
    @DisplayName("키워드를 포함하는 정의를 대소문자 무시하고 조회한다")
    void 키워드를_포함하는_정의를_대소문자_무시하고_조회한다() {
      // given
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("컬러"));
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("사이즈"));
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("소재"));

      // when
      List<ClothesAttributeDef> result =
          clothesAttributeDefRepository.findAllByNameContainingIgnoreCase(
              "사이", Sort.by(Sort.Direction.ASC, "name"));

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("사이즈");
    }

    @Test
    @DisplayName("일치하는 정의가 없으면 빈 리스트를 반환한다")
    void 일치하는_정의가_없으면_빈_리스트를_반환한다() {
      // given
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("컬러"));

      // when
      List<ClothesAttributeDef> result =
          clothesAttributeDefRepository.findAllByNameContainingIgnoreCase(
              "사이즈", Sort.by(Sort.Direction.ASC, "name"));

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("정렬 기준에 따라 결과가 정렬된다")
    void 정렬_기준에_따라_결과가_정렬된다() {
      // given
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("겨울소재"));
      clothesAttributeDefRepository.save(ClothesAttributeDef.create("여름소재"));

      // when
      List<ClothesAttributeDef> result =
          clothesAttributeDefRepository.findAllByNameContainingIgnoreCase(
              "소재", Sort.by(Sort.Direction.DESC, "name"));

      // then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getName()).isEqualTo("여름소재");
      assertThat(result.get(1).getName()).isEqualTo("겨울소재");
    }
  }
}