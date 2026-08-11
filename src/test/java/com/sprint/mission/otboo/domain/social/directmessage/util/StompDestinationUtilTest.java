package com.sprint.mission.otboo.domain.social.directmessage.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("StompDestinationUtil")
class StompDestinationUtilTest {

  static final UUID SMALLER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  static final UUID LARGER = UUID.fromString("99999999-9999-9999-9999-999999999999");
  static final String EXPECTED_DESTINATION =
      "/sub/direct-messages_11111111-1111-1111-1111-111111111111"
          + "_99999999-9999-9999-9999-999999999999";

  @Nested
  @DisplayName("directMessageDestination")
  class DirectMessageDestination {

    @Test
    @DisplayName("두 사용자 ID를 사전순으로 정렬해 destination을 만든다")
    void 두_사용자_ID를_사전순으로_정렬해_destination을_만든다() {
      // when & then
      assertThat(StompDestinationUtil.directMessageDestination(SMALLER, LARGER))
          .isEqualTo(EXPECTED_DESTINATION);
    }

    @Test
    @DisplayName("인자 순서가 반대여도 동일한 destination을 만든다")
    void 인자_순서가_반대여도_동일한_destination을_만든다() {
      // when & then
      assertThat(StompDestinationUtil.directMessageDestination(LARGER, SMALLER))
          .isEqualTo(EXPECTED_DESTINATION);
    }
  }
}