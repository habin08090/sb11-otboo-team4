package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.dto.RecommendationDto;
import com.sprint.mission.otboo.domain.clothesrecommend.recommendation.service.RecommendationService;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RecommendationService recommendationService;

  @Nested
  @DisplayName("추천 조회")
  class GetRecommendation {

    @Test
    @WithMockUser
    @DisplayName("유효한_weatherId로_조회하면_200과_추천_결과를_반환한다")
    void 유효한_weatherId로_조회하면_200과_추천_결과를_반환한다() throws Exception {
      // given
      UUID weatherId = UUID.randomUUID();
      UUID clothesId = UUID.randomUUID();

      OotdDto ootd = new OotdDto(clothesId, "반팔 티셔츠", null, ClothesType.TOP, List.of());
      RecommendationDto dto = new RecommendationDto(weatherId, UUID.randomUUID(), List.of(ootd));

      given(recommendationService.recommend(any(), any())).willReturn(dto);

      // when & then
      mockMvc.perform(get("/api/recommendations")
              .param("weatherId", weatherId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.weatherId").value(weatherId.toString()))
          .andExpect(jsonPath("$.clothes[0].name").value("반팔 티셔츠"))
          .andExpect(jsonPath("$.clothes[0].type").value("TOP"));
    }

    @Test
    @WithMockUser
    @DisplayName("weatherId가_없으면_400을_반환한다")
    void weatherId가_없으면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/recommendations"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("weatherId가_UUID_형식이_아니면_400을_반환한다")
    void weatherId가_UUID_형식이_아니면_400을_반환한다() throws Exception {
      // when & then
      mockMvc.perform(get("/api/recommendations")
              .param("weatherId", "not-a-uuid"))
          .andExpect(status().isBadRequest());
    }
  }
}