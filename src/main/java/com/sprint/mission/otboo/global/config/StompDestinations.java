package com.sprint.mission.otboo.global.config;

/**
 * STOMP destination prefix.
 *
 * <p>브로커 설정({@link WebSocketConfig})과 destination 생성이 같은 값을 참조해야 한다.
 * 한쪽만 바뀌면 컴파일도 테스트도 통과하지만 런타임에 메시지가 조용히 유실된다.
 */
public final class StompDestinations {

  public static final String BROKER_PREFIX = "/sub";
  public static final String APPLICATION_PREFIX = "/pub";
  public static final String USER_PREFIX = "/user";

  private StompDestinations() {
  }
}
