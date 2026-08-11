package com.sprint.mission.otboo.domain.social.directmessage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record DirectMessageSendRequest(
    @NotNull UUID senderId,
    @NotNull UUID receiverId,
    @NotBlank @Size(max = 100) String content
) {

}