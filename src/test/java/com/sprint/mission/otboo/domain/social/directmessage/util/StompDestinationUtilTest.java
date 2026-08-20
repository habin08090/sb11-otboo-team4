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

  @Nested
  @DisplayName("isDirectMessageParticipant")
  class IsDirectMessageParticipant {

    @Test
    @DisplayName("대화 당사자면 true를 반환한다")
    void 대화_당사자면_true를_반환한다() {
      // given
      UUID me = UUID.randomUUID();
      UUID other = UUID.randomUUID();
      String destination = StompDestinationUtil.directMessageDestination(me, other);

      // when & then
      assertThat(StompDestinationUtil.isDirectMessageParticipant(destination, me)).isTrue();
      assertThat(StompDestinationUtil.isDirectMessageParticipant(destination, other)).isTrue();
    }

    @Test
    @DisplayName("제3자면 false를 반환한다")
    void 제3자면_false를_반환한다() {
      // given
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      UUID stranger = UUID.randomUUID();
      String destination = StompDestinationUtil.directMessageDestination(a, b);

      // when & then
      assertThat(StompDestinationUtil.isDirectMessageParticipant(destination, stranger))
          .isFalse();
    }

    @Test
    @DisplayName("형식이 어긋난 destination은 거절한다")
    void 형식이_어긋난_destination은_거절한다() {
      // given
      UUID userId = UUID.randomUUID();

      // when & then
      assertThat(StompDestinationUtil.isDirectMessageParticipant(
          "/sub/direct-messages_", userId)).isFalse();
      assertThat(StompDestinationUtil.isDirectMessageParticipant(
          "/sub/direct-messages_" + userId, userId)).isFalse();
      assertThat(StompDestinationUtil.isDirectMessageParticipant(
          "/sub/notifications", userId)).isFalse();
      assertThat(StompDestinationUtil.isDirectMessageParticipant(null, userId)).isFalse();
    }

    @Test
    @DisplayName("UUID 형식이 아니면 거절한다")
    void UUID_형식이_아니면_거절한다() {
      // given
      UUID userId = UUID.randomUUID();
      String destination = "/sub/direct-messages_" + userId + "_not-a-uuid";

      // when & then
      assertThat(StompDestinationUtil.isDirectMessageParticipant(destination, userId))
          .isFalse();
    }

    @Test
    @DisplayName("사전순이 아닌 destination은 거절한다")
    void 사전순이_아닌_destination은_거절한다() {
      // given
      UUID a = UUID.randomUUID();
      UUID b = UUID.randomUUID();
      String smaller = a.toString().compareTo(b.toString()) < 0 ? a.toString() : b.toString();
      String larger = a.toString().compareTo(b.toString()) < 0 ? b.toString() : a.toString();

      // when & then
      assertThat(StompDestinationUtil.isDirectMessageParticipant(
          "/sub/direct-messages_" + larger + "_" + smaller, a)).isFalse();
    }
  }
}
