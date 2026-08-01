package com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.querydsl.ClothesCustomRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesRepository extends JpaRepository<Clothes, UUID>,
    ClothesCustomRepository {

}