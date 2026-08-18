package com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record ClothesExtractionParams(
    @NotBlank @URL String url
) {

}
