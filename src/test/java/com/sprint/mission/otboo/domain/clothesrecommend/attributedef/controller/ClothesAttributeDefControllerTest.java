package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.AttributeDefSortBy;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefCreateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefUpdateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.service.ClothesAttributeDefService;
import com.sprint.mission.otboo.global.dto.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClothesAttributeDefController.class)
class ClothesAttributeDefControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ClothesAttributeDefService clothesAttributeDefService;

  @Nested
  @DisplayName("Create")
  class Create {

    @Test
    @DisplayName("Should return 201 with created dto")
    void createSuccess() throws Exception {
      // given
      UUID definitionId = UUID.randomUUID();
      ClothesAttributeDefCreateRequest request =
          new ClothesAttributeDefCreateRequest("색상", List.of("블랙", "화이트"));
      ClothesAttributeDefDto responseDto = new ClothesAttributeDefDto(
          definitionId, "색상", List.of("블랙", "화이트"), Instant.now());

      given(clothesAttributeDefService.create(any())).willReturn(responseDto);

      // when & then
      mockMvc.perform(post("/api/clothes/attribute-defs")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.id").value(definitionId.toString()))
          .andExpect(jsonPath("$.name").value("색상"))
          .andExpect(jsonPath("$.selectableValues[0]").value("블랙"))
          .andExpect(jsonPath("$.selectableValues[1]").value("화이트"));
    }
  }

  @Nested
  @DisplayName("Update")
  class Update {

    @Test
    @DisplayName("Should return 200 with updated dto")
    void updateSuccess() throws Exception {
      // given
      UUID definitionId = UUID.randomUUID();
      ClothesAttributeDefUpdateRequest request =
          new ClothesAttributeDefUpdateRequest("컬러", List.of("빨강", "파랑"));
      ClothesAttributeDefDto responseDto = new ClothesAttributeDefDto(
          definitionId, "컬러", List.of("빨강", "파랑"), Instant.now());

      given(clothesAttributeDefService.update(eq(definitionId), any()))
          .willReturn(responseDto);

      // when & then
      mockMvc.perform(patch("/api/clothes/attribute-defs/{definitionId}", definitionId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(definitionId.toString()))
          .andExpect(jsonPath("$.name").value("컬러"))
          .andExpect(jsonPath("$.selectableValues[0]").value("빨강"))
          .andExpect(jsonPath("$.selectableValues[1]").value("파랑"));
    }
  }

  private ClothesAttributeDefListParams captureParams() {
    ArgumentCaptor<ClothesAttributeDefListParams> paramsCaptor =
        ArgumentCaptor.forClass(ClothesAttributeDefListParams.class);
    verify(clothesAttributeDefService).getAll(paramsCaptor.capture());
    return paramsCaptor.getValue();
  }

  @Nested
  @DisplayName("목록 조회")
  class GetAll {

    @Test
    @DisplayName("sortBy에_name을_보내면_NAME으로_변환해_전달한다")
    void sortBy에_name을_보내면_NAME으로_변환해_전달한다() throws Exception {
      // given
      given(clothesAttributeDefService.getAll(any())).willReturn(List.of());

      // when
      mockMvc.perform(get("/api/clothes/attribute-defs")
              .param("sortBy", "name")
              .param("sortDirection", "ASCENDING"))
          .andExpect(status().isOk());

      // then
      assertThat(captureParams().sortBy()).isEqualTo(AttributeDefSortBy.NAME);
      assertThat(captureParams().sortDirection()).isEqualTo(SortDirection.ASCENDING);
    }

    @Test
    @DisplayName("sortBy에_createdAt을_보내면_CREATED_AT으로_변환해_전달한다")
    void sortBy에_createdAt을_보내면_CREATED_AT으로_변환해_전달한다() throws Exception {
      // given
      given(clothesAttributeDefService.getAll(any())).willReturn(List.of());

      // when
      mockMvc.perform(get("/api/clothes/attribute-defs")
              .param("sortBy", "createdAt")
              .param("sortDirection", "DESCENDING"))
          .andExpect(status().isOk());

      // then
      assertThat(captureParams().sortBy()).isEqualTo(AttributeDefSortBy.CREATED_AT);
      assertThat(captureParams().sortDirection()).isEqualTo(SortDirection.DESCENDING);
    }
  }

  @Nested
  @DisplayName("Delete")
  class Delete {

    @Test
    @DisplayName("Should return 204 No Content")
    void deleteSuccess() throws Exception {
      // given
      UUID definitionId = UUID.randomUUID();

      // when & then
      mockMvc.perform(delete("/api/clothes/attribute-defs/{definitionId}", definitionId))
          .andExpect(status().isNoContent());

      verify(clothesAttributeDefService).delete(definitionId);
    }
  }
}