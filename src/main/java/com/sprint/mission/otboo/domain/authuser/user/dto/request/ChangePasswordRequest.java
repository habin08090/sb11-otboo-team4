package com.sprint.mission.otboo.domain.authuser.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$", message = "비밀번호는 영문, 숫자를 포함하여 6자 이상이어야 합니다.")
    String password
) {

}
