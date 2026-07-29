package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto;

import java.util.List;

public record ClothesAttributeDefUpdateRequest(
    String name,
    List<String> selectableValues
) {}