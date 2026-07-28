package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.repository;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDefValue;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothesAttributeDefValueRepository
    extends JpaRepository<ClothesAttributeDefValue, UUID> {

  @Query("""
      select v
      from ClothesAttributeDefValue v
      join fetch v.definition d
      where d.id in :definitionIds
      order by v.sortOrder asc
      """)
  List<ClothesAttributeDefValue> findAllByDefinitionIds(
      @Param("definitionIds") List<UUID> definitionIds);
}