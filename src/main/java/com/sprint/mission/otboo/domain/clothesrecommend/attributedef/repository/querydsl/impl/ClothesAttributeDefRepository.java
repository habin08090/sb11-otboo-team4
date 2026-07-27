package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID> {

  boolean existsByName(String name);

  List<ClothesAttributeDef> findAllByNameContainingIgnoreCase(String keyword, Sort sort);
}