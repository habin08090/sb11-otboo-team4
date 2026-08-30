package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 의상 이름 길이 제약이 DB 컬럼(`clothes.name VARCHAR(255)`)과 일치하는지 확인한다.
 *
 * <p>DTO가 DB보다 넉넉하면 검증을 통과한 요청이 DB에서 터져 500이 된다. 구매 링크에서 불러온 상품명이 그 구간에 자주 들어가 실제로 문제가 됐다.
 */
@DisplayName("의상 이름 길이 제약")
class ClothesNameLengthTest {

  private static final int NAME_MAX_LENGTH = 255;

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();

  private static String nameOfLength(int length) {
    return "가".repeat(length);
  }

  private static Set<String> violatedFields(Object target) {
    return VALIDATOR.validate(target).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  @Nested
  @DisplayName("등록 요청")
  class Create {

    private static ClothesCreateRequest request(String name) {
      return new ClothesCreateRequest(UUID.randomUUID(), name, ClothesType.TOP, List.of());
    }

    @Test
    @DisplayName("이름이 255자면 통과한다")
    void 이름이_255자면_통과한다() {
      // given
      ClothesCreateRequest request = request(nameOfLength(NAME_MAX_LENGTH));

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).isEmpty();
    }

    @Test
    @DisplayName("구매 링크에서 불러온 길이의 상품명도 통과한다")
    void 구매_링크에서_불러온_길이의_상품명도_통과한다() {
      // given - 실제 무신사 페이지 제목 (65자)
      String extracted =
          "피지컬 디파트먼트(PHYSICAL DEPARTMENT) 나일론 사커 롱슬리브_다크 블루 - 사이즈 & 후기 | 무신사";
      ClothesCreateRequest request = request(extracted);

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).isEmpty();
    }

    @Test
    @DisplayName("이름이 256자면 name에 위반이 잡힌다")
    void 이름이_256자면_name에_위반이_잡힌다() {
      // given
      ClothesCreateRequest request = request(nameOfLength(NAME_MAX_LENGTH + 1));

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).contains("name");
    }

    @Test
    @DisplayName("이름이 비면 name에 위반이 잡힌다")
    void 이름이_비면_name에_위반이_잡힌다() {
      // given
      ClothesCreateRequest request = request("   ");

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).contains("name");
    }
  }

  @Nested
  @DisplayName("수정 요청")
  class Update {

    private static ClothesUpdateRequest request(String name) {
      return new ClothesUpdateRequest(name, ClothesType.TOP, List.of());
    }

    @Test
    @DisplayName("이름이 255자면 통과한다")
    void 이름이_255자면_통과한다() {
      // given
      ClothesUpdateRequest request = request(nameOfLength(NAME_MAX_LENGTH));

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).isEmpty();
    }

    @Test
    @DisplayName("이름이 256자면 name에 위반이 잡힌다")
    void 이름이_256자면_name에_위반이_잡힌다() {
      // given
      ClothesUpdateRequest request = request(nameOfLength(NAME_MAX_LENGTH + 1));

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).contains("name");
    }

    @Test
    @DisplayName("이름을 보내지 않으면 위반이 없다")
    void 이름을_보내지_않으면_위반이_없다() {
      // given - 수정은 부분 갱신이라 name이 null이어도 된다
      ClothesUpdateRequest request = request(null);

      // when
      Set<String> violatedFields = violatedFields(request);

      // then
      assertThat(violatedFields).isEmpty();
    }
  }
}
