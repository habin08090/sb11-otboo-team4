package com.sprint.mission.otboo.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.social.directmessage.controller.DirectMessageStompController;
import com.sprint.mission.otboo.domain.social.directmessage.dto.DirectMessageSendRequest;
import com.sprint.mission.otboo.domain.social.directmessage.exception.DirectMessageForbiddenException;
import com.sprint.mission.otboo.global.dto.ErrorResponse;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

@DisplayName("StompExceptionHandler")
class StompExceptionHandlerTest {

  StompExceptionHandler handler = new StompExceptionHandler();

  @Nested
  @DisplayName("예외 처리")
  class HandleException {

    @Test
    @DisplayName("OtbooException이 발생하면 예외 정보를 담은 ErrorResponse를 반환한다")
    void OtbooException이_발생하면_예외_정보를_담은_ErrorResponse를_반환한다() {
      // given
      DirectMessageForbiddenException exception =
          DirectMessageForbiddenException.senderMismatch();

      // when
      ErrorResponse result = handler.handleOtbooException(exception);

      // then
      assertThat(result.exceptionName()).isEqualTo("DirectMessageForbiddenException");
      assertThat(result.message()).isEqualTo(exception.getMessage());
    }

    @Test
    @DisplayName("검증 실패 시 필드별 메시지를 담은 ErrorResponse를 반환한다")
    void 검증_실패_시_필드별_메시지를_담은_ErrorResponse를_반환한다() throws Exception {
      // given
      DirectMessageSendRequest request =
          new DirectMessageSendRequest(UUID.randomUUID(), UUID.randomUUID(), "");
      BeanPropertyBindingResult bindingResult =
          new BeanPropertyBindingResult(request, "directMessageSendRequest");
      bindingResult.addError(new FieldError(
          "directMessageSendRequest", "content", "공백일 수 없습니다"));

      MethodParameter parameter = new MethodParameter(
          DirectMessageStompController.class.getDeclaredMethod(
              "send", DirectMessageSendRequest.class, Principal.class), 0);
      MethodArgumentNotValidException exception =
          new MethodArgumentNotValidException(
              MessageBuilder.withPayload("").build(), parameter, bindingResult);

      // when
      ErrorResponse result = handler.handleValidationException(exception);

      // then
      assertThat(result.exceptionName()).isEqualTo("MethodArgumentNotValidException");
      assertThat(result.message()).isEqualTo("요청 값이 유효하지 않습니다.");
      assertThat(result.details()).containsEntry("content", "공백일 수 없습니다");
    }

    @Test
    @DisplayName("처리되지 않은 예외는 내부 상세를 감춘 ErrorResponse로 변환한다")
    void 처리되지_않은_예외는_내부_상세를_감춘_ErrorResponse로_변환한다() {
      // given
      Exception exception = new IllegalStateException("DB 커넥션 풀 고갈");

      // when
      ErrorResponse result = handler.handleException(exception);

      // then
      assertThat(result.exceptionName()).isEqualTo("IllegalStateException");
      assertThat(result.message()).isEqualTo("메시지 처리 중 오류가 발생했습니다.");
      assertThat(result.details()).isNull();
    }
  }
}
