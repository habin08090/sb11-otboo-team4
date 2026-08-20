package com.sprint.mission.otboo.global.exception;

import com.sprint.mission.otboo.global.dto.ErrorResponse;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * STOMP 메시지 처리 중 발생한 예외를 사용자별 에러 채널로 전달한다.
 *
 * <p>ERROR 프레임은 STOMP 명세상 연결 종료를 수반하므로, 애플리케이션 검증 실패로 소켓이
 * 끊기지 않도록 {@code @SendToUser}를 사용한다. 응답 형식은 {@link GlobalExceptionHandler}와 같다.
 */
@Slf4j
@ControllerAdvice
public class StompExceptionHandler {

  @MessageExceptionHandler(OtbooException.class)
  @SendToUser("/queue/errors")
  public ErrorResponse handleOtbooException(OtbooException e) {
    log.warn("[{}] {}", e.getClass().getSimpleName(), e.getMessage());
    return new ErrorResponse(e.getClass().getSimpleName(), e.getMessage(), e.getDetails());
  }

  @MessageExceptionHandler(MethodArgumentNotValidException.class)
  @SendToUser("/queue/errors")
  public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
    Map<String, Object> details = e.getBindingResult() == null ? Map.of()
        : e.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                (existing, replacement) -> existing
            ));
    log.warn("[{}] {}", e.getClass().getSimpleName(), details);
    return new ErrorResponse(e.getClass().getSimpleName(), "요청 값이 유효하지 않습니다.", details);
  }

  // 폴백이 없으면 처리되지 않은 예외에서 Spring이 로그만 남기고 끝나, 클라이언트는 응답을 받지 못한다.
  @MessageExceptionHandler(Exception.class)
  @SendToUser("/queue/errors")
  public ErrorResponse handleException(Exception e) {
    log.error("[{}] {}", e.getClass().getSimpleName(), e.getMessage(), e);
    return new ErrorResponse(
        e.getClass().getSimpleName(), "메시지 처리 중 오류가 발생했습니다.", null);
  }
}
