package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttributeDefErrorCode {

  ATTRIBUTE_DEF_NOT_FOUND("의상 속성 정의를 찾을 수 없습니다."),
  ATTRIBUTE_DEF_NAME_DUPLICATED("이미 존재하는 의상 속성 정의 이름입니다."),
  ATTRIBUTE_DEF_SELECTABLE_VALUE_DUPLICATED("선택 가능한 값은 중복될 수 없습니다."),
  ATTRIBUTE_DEF_SELECTABLE_VALUE_EMPTY("선택 가능한 값을 1개 이상 입력해야 합니다.");

  private final String message;
}