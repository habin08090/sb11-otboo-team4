package com.sprint.mission.otboo.domain.authuser.user.dto.response;

import com.sprint.mission.otboo.domain.authuser.user.entity.enums.OAuth2Provider;

public record SocialLinkedStateDto(
    OAuth2Provider provider,
    boolean state
) {

}
