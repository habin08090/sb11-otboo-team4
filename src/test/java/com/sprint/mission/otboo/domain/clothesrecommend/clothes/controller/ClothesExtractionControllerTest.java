package com.sprint.mission.otboo.domain.clothesrecommend.clothes.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesExtractionService;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClothesController.class)
@WithMockUser
@TestPropertySource(properties = {
    "otboo.admin.name=test",
    "otboo.admin.email=test@test.com",
    "otboo.admin.password=test1234!",
    "otboo.auth.token.access-secret=c2hvdWxkLWJlLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmctc2VjcmV0LWtleS1mb3ItaHMyNTY=",
    "otboo.auth.token.refresh-secret=c2hvdWxkLWJlLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmctc2VjcmV0LWtleS1mb3ItaHMyNTY=",
    "otboo.auth.token.access-token-expiration-minutes=15",
    "otboo.auth.token.refresh-token-expiration-days=14",
    "otboo.auth.token.impl=nimbus",
    "otboo.auth.user-session.impl=redis",
    "otboo.auth.refresh-cookie.secure=false"
})
@DisplayName("의상 URL 추출 컨트롤러")
class ClothesExtractionControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ClothesService clothesService;

  @MockitoBean
  ClothesExtractionService clothesExtractionService;

  @Nested
  @DisplayName("의상 URL 추출 요청: GET /api/clothes/extractions")
  class ExtractByUrl {

    @Test
    @DisplayName("정상 URL이면 200과 ClothesDto를 반환한다")
    void 정상_URL이면_200과_ClothesDto를_반환한다() throws Exception {
      // given
      ClothesDto expected = new ClothesDto(
          null, null, "데님 자켓",
          "https://image.musinsa.com/goods/001.jpg",
          null, List.of()
      );
      when(clothesExtractionService.extractByUrl(anyString())).thenReturn(expected);

      // when & then
      mockMvc.perform(get("/api/clothes/extractions")
              .param("url", "https://www.musinsa.com/products/12345")
              .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value("데님 자켓"))
          .andExpect(jsonPath("$.imageUrl").value("https://image.musinsa.com/goods/001.jpg"))
          .andExpect(jsonPath("$.id").doesNotExist())
          .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    @DisplayName("url 파라미터가 없으면 400을 반환한다")
    void url_파라미터가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/clothes/extractions")
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("url 파라미터가 빈 문자열이면 400을 반환한다")
    void url_파라미터가_빈_문자열이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/clothes/extractions")
              .param("url", "")
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }
}
