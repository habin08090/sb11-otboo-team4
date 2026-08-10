package com.sprint.mission.otboo.domain.social.feed.dto;

import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
    UUID id,
    Instant createdAt,
    UUID feedId,
    UserSummary author,
    String content
) {

}