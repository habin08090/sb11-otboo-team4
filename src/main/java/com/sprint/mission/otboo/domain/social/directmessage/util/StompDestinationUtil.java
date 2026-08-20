package com.sprint.mission.otboo.domain.social.directmessage.util;

import com.sprint.mission.otboo.global.config.StompDestinations;
import java.util.UUID;

public final class StompDestinationUtil {

  private static final String DESTINATION_PREFIX =
      StompDestinations.BROKER_PREFIX + "/direct-messages_";

  private static final String SEPARATOR = "_";
  private static final int PARTICIPANT_COUNT = 2;

  private StompDestinationUtil() {
  }

  public static String directMessageDestination(UUID one, UUID other) {
    String a = one.toString();
    String b = other.toString();
    return a.compareTo(b) < 0
        ? DESTINATION_PREFIX + a + SEPARATOR + b
        : DESTINATION_PREFIX + b + SEPARATOR + a;
  }

  /**
   * destination이 DM 구독 주소이고, 해당 사용자가 대화 당사자인지 확인한다.
   *
   * <p>알고 있는 패턴만 허용하는 화이트리스트 방식이다. 형식이 어긋나면 무조건 거절하므로
   * 새로운 구독 경로가 인가 없이 열리지 않는다.
   */
  public static boolean isDirectMessageParticipant(String destination, UUID userId) {
    if (destination == null || !destination.startsWith(DESTINATION_PREFIX)) {
      return false;
    }
    String pair = destination.substring(DESTINATION_PREFIX.length());
    String[] participants = pair.split(SEPARATOR, -1);
    if (participants.length != PARTICIPANT_COUNT) {
      return false;
    }
    try {
      UUID first = UUID.fromString(participants[0]);
      UUID second = UUID.fromString(participants[1]);
      return directMessageDestination(first, second).equals(destination)
          && (userId.equals(first) || userId.equals(second));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
