package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClothesAttributeDto(
    @NotNull UUID definitionId,
    @NotBlank String value
) {}