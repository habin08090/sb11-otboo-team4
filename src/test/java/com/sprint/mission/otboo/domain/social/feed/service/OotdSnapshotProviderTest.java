package com.sprint.mission.otboo.domain.social.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesService;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdSnapshot;
import com.sprint.mission.otboo.domain.social.feed.exception.ClothesOwnershipException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("OotdSnapshotProvider")
class OotdSnapshotProviderTest {

  static final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
      .plugin(new JakartaValidationPlugin())
      .build();

  @InjectMocks
  OotdSnapshotProvider ootdSnapshotProvider;

  @Mock
  ClothesService clothesService;

  @Nested
  @DisplayName("readOotds")
  class ReadOotds {

    @Test
    @DisplayName("clothesIds로 조회한 ClothesDto를 OotdSnapshot으로 변환해 반환한다")
    void clothesIds로_조회한_ClothesDto를_OotdSnapshot으로_변환해_반환한다() {
      // given
      UUID authorId = UUID.randomUUID();
      UUID clothesId1 = UUID.randomUUID();
      UUID clothesId2 = UUID.randomUUID();
      List<UUID> clothesIds = List.of(clothesId1, clothesId2);

      ClothesDto dto1 = fm.giveMeBuilder(ClothesDto.class)
          .set("id", clothesId1)
          .set("ownerId", authorId)
          .set("name", "패딩")
          .sample();
      ClothesDto dto2 = fm.giveMeBuilder(ClothesDto.class)
          .set("id", clothesId2)
          .set("ownerId", authorId)
          .set("name", "청바지")
          .sample();
      when(clothesService.getClothesByIds(clothesIds)).thenReturn(List.of(dto1, dto2));

      // when
      List<OotdSnapshot> result = ootdSnapshotProvider.readOotds(clothesIds, authorId);

      // then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).clothesId()).isEqualTo(clothesId1);
      assertThat(result.get(0).name()).isEqualTo("패딩");
      assertThat(result.get(1).clothesId()).isEqualTo(clothesId2);
      assertThat(result.get(1).name()).isEqualTo("청바지");
    }

    @Test
    @DisplayName("빈 clothesIds면 빈 리스트를 반환한다")
    void 빈_clothesIds면_빈_리스트를_반환한다() {
      // given
      UUID authorId = UUID.randomUUID();

      // when
      List<OotdSnapshot> result = ootdSnapshotProvider.readOotds(List.of(), authorId);

      // then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("일부 clothesId가 존재하지 않으면 해당 항목은 결과에서 제외된다")
    void 일부_clothesId가_존재하지_않으면_해당_항목은_결과에서_제외된다() {
      // given
      UUID authorId = UUID.randomUUID();
      UUID existingId = UUID.randomUUID();
      UUID missingId = UUID.randomUUID();
      List<UUID> clothesIds = List.of(existingId, missingId);

      ClothesDto dto = fm.giveMeBuilder(ClothesDto.class)
          .set("id", existingId)
          .set("ownerId", authorId)
          .set("name", "패딩")
          .sample();
      // getClothesByIds는 없는 id를 조용히 제외 (ClothesService 구현)
      when(clothesService.getClothesByIds(clothesIds)).thenReturn(List.of(dto));

      // when
      List<OotdSnapshot> result = ootdSnapshotProvider.readOotds(clothesIds, authorId);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).clothesId()).isEqualTo(existingId);
    }

    @Test
    @DisplayName("작성자 소유가 아닌 의상이 포함되면 ClothesOwnershipException을 던진다")
    void 작성자_소유가_아닌_의상이_포함되면_ClothesOwnershipException을_던진다() {
      // given
      UUID authorId = UUID.randomUUID();
      UUID otherOwnerId = UUID.randomUUID();
      UUID clothesId1 = UUID.randomUUID();
      UUID clothesId2 = UUID.randomUUID();
      List<UUID> clothesIds = List.of(clothesId1, clothesId2);

      ClothesDto mine = fm.giveMeBuilder(ClothesDto.class)
          .set("id", clothesId1)
          .set("ownerId", authorId)
          .sample();
      ClothesDto others = fm.giveMeBuilder(ClothesDto.class)
          .set("id", clothesId2)
          .set("ownerId", otherOwnerId)
          .sample();
      when(clothesService.getClothesByIds(clothesIds)).thenReturn(List.of(mine, others));

      // when & then
      assertThatThrownBy(() -> ootdSnapshotProvider.readOotds(clothesIds, authorId))
          .isInstanceOf(ClothesOwnershipException.class)
          .satisfies(ex -> {
            ClothesOwnershipException e = (ClothesOwnershipException) ex;
            assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(e.getDetails()).isEmpty();
          });
    }
  }
}