package com.sprint.mission.otboo.domain.clothesrecommend.attributedef.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefDto;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.dto.ClothesAttributeDefUpdateRequest;
import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.service.ClothesAttributeDefService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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