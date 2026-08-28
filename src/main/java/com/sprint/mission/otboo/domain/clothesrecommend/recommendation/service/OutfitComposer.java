package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 추천 후보군에서 한 벌의 조합을 만든다.
 *
 * <p>같은 후보군이라도 호출할 때마다 다른 조합이 나온다. FE의 "다른 옷 추천"이 같은 요청을 다시 보내는 구조라, 다양성은 서버가 만들어야 한다.
 */
@Component
public class OutfitComposer {

  private final Supplier<RandomGenerator> randomSupplier;

  public OutfitComposer() {
    this(ThreadLocalRandom::current);
  }

  OutfitComposer(Supplier<RandomGenerator> randomSupplier) {
    this.randomSupplier = randomSupplier;
  }

  /**
   * 후보군에서 추천 종류마다 한 벌씩 무작위로 골라 조합을 만든다.
   *
   * <p>원피스는 상·하의를 대체하므로 셋을 함께 담지 않는다. 원피스와 상·하의가 모두 가능하면 둘 중 하나를 무작위로 고른다.
   */
  public List<Clothes> compose(List<Clothes> pool, Set<ClothesType> recommendedTypes) {
    RandomGenerator random = randomSupplier.get();
    Map<ClothesType, List<Clothes>> byType = groupByType(pool, recommendedTypes);

    List<Clothes> selected = new ArrayList<>();
    for (ClothesType type : resolveTypes(byType.keySet(), random)) {
      selected.add(pickOne(byType.get(type), random));
    }
    return List.copyOf(selected);
  }

  private Map<ClothesType, List<Clothes>> groupByType(List<Clothes> pool,
      Set<ClothesType> recommendedTypes) {
    return pool.stream()
        .filter(clothes -> recommendedTypes.contains(clothes.getType()))
        .collect(Collectors.groupingBy(Clothes::getType,
            () -> new EnumMap<>(ClothesType.class), Collectors.toList()));
  }

  private List<ClothesType> resolveTypes(Set<ClothesType> availableTypes, RandomGenerator random) {
    boolean hasDress = availableTypes.contains(ClothesType.DRESS);
    boolean hasSeparates = availableTypes.contains(ClothesType.TOP)
        || availableTypes.contains(ClothesType.BOTTOM);

    boolean wearDress = hasDress && (!hasSeparates || random.nextBoolean());

    List<ClothesType> types = new ArrayList<>();
    for (ClothesType type : availableTypes) {
      boolean excluded = wearDress
          ? type == ClothesType.TOP || type == ClothesType.BOTTOM
          : type == ClothesType.DRESS;
      if (!excluded) {
        types.add(type);
      }
    }
    return types;
  }

  private Clothes pickOne(List<Clothes> candidates, RandomGenerator random) {
    return candidates.get(random.nextInt(candidates.size()));
  }
}
