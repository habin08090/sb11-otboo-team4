package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.AttributeDefSortBy;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDefValue;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception.ClothesAttributeDefNameDuplicatedException;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.mapper.ClothesAttributeDefMapper;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository.ClothesAttributeDefRepository;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository.ClothesAttributeDefValueRepository;
import com.sprint.mission.otboo.global.dto.SortDirection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClothesAttributeDefServiceTest")
class ClothesAttributeDefServiceTest {

  @InjectMocks
  ClothesAttributeDefService clothesAttributeDefService;

  @Mock
  ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Mock
  ClothesAttributeDefValueRepository clothesAttributeDefValueRepository;

  @Spy
  ClothesAttributeDefMapper clothesAttributeDefMapper = new ClothesAttributeDefMapper();

  @Nested
  @DisplayName("의상 속성 정의 등록")
  class Create {

    @Test
    @DisplayName("정상 요청이면 정의와 선택 값이 저장된다")
    void 정상_요청이면_정의와_선택_값이_저장된다() {
      // given
      ClothesAttributeDefCreateRequest request =
          new ClothesAttributeDefCreateRequest("색상", List.of("빨강", "파랑", "초록"));
      given(clothesAttributeDefRepository.existsByName("색상")).willReturn(false);
      given(clothesAttributeDefRepository.save(any(ClothesAttributeDef.class)))
          .willAnswer(invocation -> invocation.getArgument(0));
      given(clothesAttributeDefValueRepository.saveAll(anyList()))
          .willAnswer(invocation -> invocation.getArgument(0));

      // when
      ClothesAttributeDefDto result = clothesAttributeDefService.create(request);

      // then
      assertThat(result.name()).isEqualTo("색상");
      assertThat(result.selectableValues()).containsExactly("빨강", "파랑", "초록");
      assertThat(result.id()).isNotNull();
    }

    @Test
    @DisplayName("같은 이름의 정의가 이미 있으면 예외가 발생한다")
    void 같은_이름의_정의가_이미_있으면_예외가_발생한다() {
      // given
      ClothesAttributeDefCreateRequest request =
          new ClothesAttributeDefCreateRequest("색상", List.of("빨강"));
      given(clothesAttributeDefRepository.existsByName("색상")).willReturn(true);

      // when & then
      assertThatThrownBy(() -> clothesAttributeDefService.create(request))
          .isInstanceOf(ClothesAttributeDefNameDuplicatedException.class);
      verify(clothesAttributeDefRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("의상 속성 정의 목록 조회")
  class GetAll {

    @Test
    @DisplayName("전체 조회 시 정의별 선택 값을 함께 반환한다")
    void 전체_조회_시_정의별_선택_값을_함께_반환한다() {
      // given
      ClothesAttributeDef color = ClothesAttributeDef.create("색상");
      ClothesAttributeDef thickness = ClothesAttributeDef.create("두께감");
      given(clothesAttributeDefRepository.findAll(any(Sort.class)))
          .willReturn(List.of(color, thickness));
      given(clothesAttributeDefValueRepository.findAllByDefinitionIds(anyList()))
          .willReturn(List.of(
              ClothesAttributeDefValue.create(color, "빨강", 0),
              ClothesAttributeDefValue.create(color, "파랑", 1),
              ClothesAttributeDefValue.create(thickness, "얇음", 0)));

      ClothesAttributeDefListParams params = new ClothesAttributeDefListParams(
          AttributeDefSortBy.CREATED_AT, SortDirection.ASCENDING, null);

      // when
      List<ClothesAttributeDefDto> result = clothesAttributeDefService.getAll(params);

      // then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).selectableValues()).containsExactly("빨강", "파랑");
      assertThat(result.get(1).selectableValues()).containsExactly("얇음");
    }

    @Test
    @DisplayName("검색어가 있으면 이름 부분 일치로 조회한다")
    void 검색어가_있으면_이름_부분_일치로_조회한다() {
      // given
      ClothesAttributeDef color = ClothesAttributeDef.create("색상");
      given(clothesAttributeDefRepository.findAllByNameContainingIgnoreCase(
          eq("색"), any(Sort.class)))
          .willReturn(List.of(color));
      given(clothesAttributeDefValueRepository.findAllByDefinitionIds(anyList()))
          .willReturn(List.of(ClothesAttributeDefValue.create(color, "빨강", 0)));

      ClothesAttributeDefListParams params = new ClothesAttributeDefListParams(
          AttributeDefSortBy.NAME, SortDirection.DESCENDING, "색");

      // when
      List<ClothesAttributeDefDto> result = clothesAttributeDefService.getAll(params);

      // then
      assertThat(result).hasSize(1);
      verify(clothesAttributeDefRepository, never()).findAll(any(Sort.class));
    }

    @Test
    @DisplayName("정의가 없으면 값 조회 없이 빈 목록을 반환한다")
    void 정의가_없으면_값_조회_없이_빈_목록을_반환한다() {
      // given
      given(clothesAttributeDefRepository.findAll(any(Sort.class)))
          .willReturn(List.of());
      ClothesAttributeDefListParams params = new ClothesAttributeDefListParams(
          AttributeDefSortBy.CREATED_AT, SortDirection.ASCENDING, null);

      // when
      List<ClothesAttributeDefDto> result = clothesAttributeDefService.getAll(params);

      // then
      assertThat(result).isEmpty();
      verify(clothesAttributeDefValueRepository, never()).findAllByDefinitionIds(anyList());
    }
  }
}