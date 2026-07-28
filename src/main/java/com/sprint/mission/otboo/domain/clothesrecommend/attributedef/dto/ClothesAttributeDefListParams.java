package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto;

import com.sprint.mission.otboo.global.dto.SortDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClothesAttributeDefListParams(
    @NotNull
    AttributeDefSortBy sortBy,

    @NotNull
    SortDirection sortDirection,

    @Size(max = 50)
    String keywordLike
) {

}