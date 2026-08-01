package com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.querydsl;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;

public interface ClothesCustomRepository {

  CursorPageResponse<Clothes> findClothes(ClothesListParams params);
}