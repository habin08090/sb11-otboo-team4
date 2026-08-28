package com.sprint.mission.otboo.domain.clothesrecommend.clothes.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesDto;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesUpdateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.exception.ClothesNotFoundException;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesExtractionService;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.service.ClothesService;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import com.sprint.mission.otboo.global.dto.SortDirection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClothesController.class)
@ActiveProfiles("test")
@WithMockUser
@DisplayName("의상 컨트롤러")
class ClothesControllerTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ClothesService clothesService;

  @MockitoBean
  ClothesExtractionService clothesExtractionService;

  @Nested
  @DisplayName("옷 목록 조회: GET /api/clothes")
  class GetClothes {

    @Test
    @DisplayName("정상 요청이면 200과 커서 페이지를 반환한다")
    void 정상_요청이면_200과_커서_페이지를_반환한다() throws Exception {
      // given
      UUID ownerId = UUID.randomUUID();
      UUID clothesId = UUID.randomUUID();

      ClothesDto dto = new ClothesDto(
          clothesId, ownerId, "반팔티", null, ClothesType.TOP, List.of());
      CursorPageResponse<ClothesDto> page = new CursorPageResponse<>(
          List.of(dto), null, null, false, 1L, "createdAt", SortDirection.DESCENDING);
      given(clothesService.getClothes(any(ClothesListParams.class))).willReturn(page);

      // when & then
      mockMvc.perform(get("/api/clothes")
              .param("ownerId", ownerId.toString())
              .param("limit", "20")
              .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].id").value(clothesId.toString()))
          .andExpect(jsonPath("$.data[0].name").value("반팔티"))
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("ownerId가 없으면 400을 반환한다")
    void ownerId가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/clothes")
              .param("limit", "20")
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("limit이 허용 범위를 벗어나면 400을 반환한다")
    void limit이_허용_범위를_벗어나면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/clothes")
              .param("ownerId", UUID.randomUUID().toString())
              .param("limit", "101")
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("옷 등록: POST /api/clothes")
  class CreateClothes {

    @Test
    @DisplayName("정상 요청이면 201과 ClothesDto를 반환한다")
    void 정상_요청이면_201과_ClothesDto를_반환한다() throws Exception {
      // given
      UUID ownerId = UUID.randomUUID();
      UUID clothesId = UUID.randomUUID();

      ClothesDto expectedDto = new ClothesDto(
          clothesId, ownerId, "반팔티", null, ClothesType.TOP, List.of());
      given(clothesService.create(any(ClothesCreateRequest.class), any()))
          .willReturn(expectedDto);

      String requestJson = """
          {
            "ownerId": "%s",
            "name": "반팔티",
            "type": "TOP",
            "attributes": []
          }
          """.formatted(ownerId);

      MockMultipartFile requestPart = new MockMultipartFile(
          "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
          requestJson.getBytes(StandardCharsets.UTF_8));

      // when & then
      mockMvc.perform(multipart("/api/clothes")
              .file(requestPart)
              .with(csrf()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(clothesId.toString()))
          .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
          .andExpect(jsonPath("$.name").value("반팔티"))
          .andExpect(jsonPath("$.type").value("TOP"));
    }
  }

  @Nested
  @DisplayName("옷 수정: PATCH /api/clothes/{clothesId}")
  class UpdateClothes {

    @Test
    @DisplayName("정상 요청이면 200과 ClothesDto를 반환한다")
    void 정상_요청이면_200과_ClothesDto를_반환한다() throws Exception {
      // given
      UUID clothesId = UUID.randomUUID();
      UUID ownerId = UUID.randomUUID();

      ClothesDto expectedDto = new ClothesDto(
          clothesId, ownerId, "긴팔티", null, ClothesType.OUTER, List.of());
      given(clothesService.update(eq(clothesId), any(ClothesUpdateRequest.class), any()))
          .willReturn(expectedDto);

      String requestJson = """
          {
            "name": "긴팔티",
            "type": "OUTER",
            "attributes": []
          }
          """;

      MockMultipartFile requestPart = new MockMultipartFile(
          "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
          requestJson.getBytes(StandardCharsets.UTF_8));

      // when & then
      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/clothes/{clothesId}", clothesId)
              .file(requestPart)
              .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(clothesId.toString()))
          .andExpect(jsonPath("$.name").value("긴팔티"))
          .andExpect(jsonPath("$.type").value("OUTER"));
    }

    @Test
    @DisplayName("존재하지 않는 옷이면 404를 반환한다")
    void 존재하지_않는_옷이면_404를_반환한다() throws Exception {
      // given
      UUID clothesId = UUID.randomUUID();

      given(clothesService.update(eq(clothesId), any(ClothesUpdateRequest.class), any()))
          .willThrow(ClothesNotFoundException.withId(clothesId));

      String requestJson = """
          {
            "name": "긴팔티"
          }
          """;

      MockMultipartFile requestPart = new MockMultipartFile(
          "request", "request.json", MediaType.APPLICATION_JSON_VALUE,
          requestJson.getBytes(StandardCharsets.UTF_8));

      // when & then
      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/clothes/{clothesId}", clothesId)
              .file(requestPart)
              .with(csrf()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("옷 삭제: DELETE /api/clothes/{clothesId}")
  class DeleteClothes {

    @Test
    @DisplayName("정상 요청이면 204를 반환한다")
    void 정상_요청이면_204를_반환한다() throws Exception {
      // given
      UUID clothesId = UUID.randomUUID();
      doNothing().when(clothesService).delete(clothesId);

      // when & then
      mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
              .with(csrf()))
          .andExpect(status().isNoContent());

      verify(clothesService).delete(clothesId);
    }

    @Test
    @DisplayName("존재하지 않는 옷이면 404를 반환한다")
    void 존재하지_않는_옷이면_404를_반환한다() throws Exception {
      // given
      UUID clothesId = UUID.randomUUID();
      doThrow(ClothesNotFoundException.withId(clothesId))
          .when(clothesService).delete(clothesId);

      // when & then
      mockMvc.perform(delete("/api/clothes/{clothesId}", clothesId)
              .with(csrf()))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("구매 링크 추출: GET /api/clothes/extractions")
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
    @Test
    @DisplayName("허용되지 않은 호스트이면 400을 반환한다")
    void 허용되지_않은_호스트이면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/clothes/extractions")
              .param("url", "https://evil.com/products/12345")
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("https가 아니면 400을 반환한다")
    void https가_아니면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/clothes/extractions")
              .param("url", "http://www.musinsa.com/products/12345")
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }
}
