package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(
    @NotNull UUID ownerId,
    @NotBlank @Size(max = 100) String name,
    @NotNull ClothesType type,
    @Valid List<ClothesAttributeDto> attributes
) {}