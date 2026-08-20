package com.sprint.mission.otboo.external.llm.dto;

import java.util.List;
import java.util.UUID;

public record LlmSelectedClothes(
    List<UUID> clothesIds
) {

}
