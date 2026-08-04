package com.sprint.mission.otboo.domain.social.feed.service;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesService;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdSnapshot;
import com.sprint.mission.otboo.domain.social.feed.exception.ClothesOwnershipException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
public class OotdSnapshotProvider {

  private final ClothesService clothesService;

  public List<OotdSnapshot> readOotds(List<UUID> clothesIds, UUID authorId) {
    if (clothesIds == null || clothesIds.isEmpty()) {
      return List.of();
    }
    List<ClothesDto> clothesList = clothesService.getClothesByIds(clothesIds);

    boolean hasOthersClothes = clothesList.stream()
        .anyMatch(clothes -> !clothes.ownerId().equals(authorId));
    if (hasOthersClothes) {
      log.warn("착장 소유권 불일치로 피드 등록 실패");
      throw ClothesOwnershipException.withNone();
    }

    log.debug("착장 조회 완료: 요청={}, 조회={}", clothesIds.size(), clothesList.size());
    return clothesList.stream()
        .map(this::toOotdSnapshot)
        .toList();
  }

  private OotdSnapshot toOotdSnapshot(ClothesDto clothes) {
    return new OotdSnapshot(
        clothes.id(),
        clothes.name(),
        clothes.imageUrl(),
        clothes.type(),
        clothes.attributes()
    );
  }
}