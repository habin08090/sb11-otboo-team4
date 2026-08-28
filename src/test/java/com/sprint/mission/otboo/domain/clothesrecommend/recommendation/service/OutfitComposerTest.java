package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("추천 조합 구성")
class OutfitComposerTest {

  private static final UUID OWNER_ID = UUID.randomUUID();

  /**
   * 무작위 선택을 검증 가능하게 만들기 위해 시드를 고정한다.
   *
   * <p>Random 인스턴스 하나를 공유해야 호출마다 난수열이 이어진다. 호출할 때마다 새로 만들면 매번 같은 값이 나와 무작위성을 검증할 수 없다.
   */
  private static OutfitComposer composerWithSeed(long seed) {
    Random random = new Random(seed);
    return new OutfitComposer(() -> random);
  }

  private static Clothes clothes(String name, ClothesType type) {
    return Clothes.create(OWNER_ID, name, type);
  }

  @Nested
  @DisplayName("조합 구성")
  class Compose {

    @Test
    @DisplayName("추천 종류마다 한 벌씩만 담는다")
    void 추천_종류마다_한_벌씩만_담는다() {
      // given
      List<Clothes> pool = List.of(
          clothes("반팔", ClothesType.TOP),
          clothes("긴팔", ClothesType.TOP),
          clothes("청바지", ClothesType.BOTTOM),
          clothes("운동화", ClothesType.SHOES));

      // when
      List<Clothes> outfit = composerWithSeed(1L)
          .compose(pool, EnumSet.of(ClothesType.TOP, ClothesType.BOTTOM, ClothesType.SHOES));

      // then
      assertThat(outfit).hasSize(3);
      assertThat(outfit.stream().map(Clothes::getType).toList()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("추천 종류에 없는 옷은 담지 않는다")
    void 추천_종류에_없는_옷은_담지_않는다() {
      // given
      List<Clothes> pool = List.of(
          clothes("반팔", ClothesType.TOP),
          clothes("목도리", ClothesType.SCARF));

      // when
      List<Clothes> outfit = composerWithSeed(1L).compose(pool, EnumSet.of(ClothesType.TOP));

      // then
      assertThat(outfit).extracting(Clothes::getType).containsExactly(ClothesType.TOP);
    }

    @Test
    @DisplayName("후보군에 없는 종류는 건너뛴다")
    void 후보군에_없는_종류는_건너뛴다() {
      // given - 신발을 추천 종류로 넣었지만 후보군에는 없다
      List<Clothes> pool = List.of(clothes("반팔", ClothesType.TOP));

      // when
      List<Clothes> outfit = composerWithSeed(1L)
          .compose(pool, EnumSet.of(ClothesType.TOP, ClothesType.SHOES));

      // then
      assertThat(outfit).extracting(Clothes::getName).containsExactly("반팔");
    }

    @Test
    @DisplayName("후보군이 비면 빈 조합을 만든다")
    void 후보군이_비면_빈_조합을_만든다() {
      // when
      List<Clothes> outfit = composerWithSeed(1L)
          .compose(List.of(), EnumSet.of(ClothesType.TOP));

      // then
      assertThat(outfit).isEmpty();
    }
  }

  @Nested
  @DisplayName("무작위성")
  class Randomness {

    @Test
    @DisplayName("같은 후보군이라도 호출할 때마다 다른 조합이 나온다")
    void 같은_후보군이라도_호출할_때마다_다른_조합이_나온다() {
      // given
      List<Clothes> pool = List.of(
          clothes("반팔", ClothesType.TOP),
          clothes("긴팔", ClothesType.TOP),
          clothes("맨투맨", ClothesType.TOP),
          clothes("청바지", ClothesType.BOTTOM),
          clothes("슬랙스", ClothesType.BOTTOM));
      Set<ClothesType> types = EnumSet.of(ClothesType.TOP, ClothesType.BOTTOM);
      OutfitComposer composer = composerWithSeed(42L);

      // when
      Set<List<String>> results = new HashSet<>();
      for (int i = 0; i < 30; i++) {
        results.add(composer.compose(pool, types).stream().map(Clothes::getName).toList());
      }

      // then
      assertThat(results).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("후보가 한 벌뿐인 종류는 항상 같은 옷이 나온다")
    void 후보가_한_벌뿐인_종류는_항상_같은_옷이_나온다() {
      // given
      List<Clothes> pool = List.of(clothes("운동화", ClothesType.SHOES));
      OutfitComposer composer = composerWithSeed(42L);

      // when & then
      for (int i = 0; i < 10; i++) {
        assertThat(composer.compose(pool, EnumSet.of(ClothesType.SHOES)))
            .extracting(Clothes::getName)
            .containsExactly("운동화");
      }
    }
  }

  @Nested
  @DisplayName("원피스와 상·하의 배타")
  class DressExclusivity {

    @Test
    @DisplayName("원피스를 고르면 상의와 하의는 담지 않는다")
    void 원피스를_고르면_상의와_하의는_담지_않는다() {
      // given
      List<Clothes> pool = List.of(
          clothes("원피스", ClothesType.DRESS),
          clothes("반팔", ClothesType.TOP),
          clothes("청바지", ClothesType.BOTTOM),
          clothes("운동화", ClothesType.SHOES));
      Set<ClothesType> types = EnumSet.of(
          ClothesType.DRESS, ClothesType.TOP, ClothesType.BOTTOM, ClothesType.SHOES);
      OutfitComposer composer = composerWithSeed(7L);

      // when & then
      for (int i = 0; i < 30; i++) {
        List<ClothesType> picked = composer.compose(pool, types).stream()
            .map(Clothes::getType).toList();

        boolean hasDress = picked.contains(ClothesType.DRESS);
        boolean hasSeparates =
            picked.contains(ClothesType.TOP) || picked.contains(ClothesType.BOTTOM);

        assertThat(hasDress && hasSeparates)
            .as("원피스와 상·하의가 동시에 담김: %s", picked)
            .isFalse();
        assertThat(hasDress || hasSeparates).isTrue();
      }
    }

    @Test
    @DisplayName("원피스와 상·하의가 모두 있으면 두 조합이 번갈아 나온다")
    void 원피스와_상하의가_모두_있으면_두_조합이_번갈아_나온다() {
      // given
      List<Clothes> pool = List.of(
          clothes("원피스", ClothesType.DRESS),
          clothes("반팔", ClothesType.TOP),
          clothes("청바지", ClothesType.BOTTOM));
      Set<ClothesType> types = EnumSet.of(
          ClothesType.DRESS, ClothesType.TOP, ClothesType.BOTTOM);
      OutfitComposer composer = composerWithSeed(7L);

      // when
      Set<Boolean> dressPicked = new HashSet<>();
      for (int i = 0; i < 30; i++) {
        dressPicked.add(composer.compose(pool, types).stream()
            .anyMatch(c -> c.getType() == ClothesType.DRESS));
      }

      // then
      assertThat(dressPicked).containsExactlyInAnyOrder(true, false);
    }

    @Test
    @DisplayName("상·하의가 없으면 원피스를 반드시 담는다")
    void 상하의가_없으면_원피스를_반드시_담는다() {
      // given
      List<Clothes> pool = List.of(
          clothes("원피스", ClothesType.DRESS),
          clothes("운동화", ClothesType.SHOES));
      Set<ClothesType> types = EnumSet.of(
          ClothesType.DRESS, ClothesType.TOP, ClothesType.BOTTOM, ClothesType.SHOES);
      OutfitComposer composer = composerWithSeed(7L);

      // when & then
      for (int i = 0; i < 20; i++) {
        assertThat(composer.compose(pool, types))
            .extracting(Clothes::getType)
            .contains(ClothesType.DRESS);
      }
    }
  }
}
