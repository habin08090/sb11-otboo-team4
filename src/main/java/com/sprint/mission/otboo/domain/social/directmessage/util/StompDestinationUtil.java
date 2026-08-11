package com.sprint.mission.otboo.domain.social.directmessage.util;

import java.util.UUID;

public final class StompDestinationUtil {

  private static final String DESTINATION_PREFIX = "/sub/direct-messages_";

  private StompDestinationUtil() {
  }

  public static String directMessageDestination(UUID one, UUID other) {
    String a = one.toString();
    String b = other.toString();
    return a.compareTo(b) < 0
        ? DESTINATION_PREFIX + a + "_" + b
        : DESTINATION_PREFIX + b + "_" + a;
  }
}