package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AttributeDefSortBy {
  @JsonProperty("createdAt")
  CREATED_AT,

  @JsonProperty("name")
  NAME
}