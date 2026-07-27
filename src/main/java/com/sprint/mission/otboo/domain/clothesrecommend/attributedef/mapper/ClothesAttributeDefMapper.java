package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.mapper;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDefValue;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClothesAttributeDefMapper {

  public ClothesAttributeDefDto toDto(
      ClothesAttributeDef definition, List<ClothesAttributeDefValue> values) {
    return new ClothesAttributeDefDto(
        definition.getId(),
        definition.getName(),
        values.stream()
            .sorted(Comparator.comparingInt(ClothesAttributeDefValue::getSortOrder))
            .map(ClothesAttributeDefValue::getValue)
            .toList(),
        definition.getCreatedAt());
  }
}