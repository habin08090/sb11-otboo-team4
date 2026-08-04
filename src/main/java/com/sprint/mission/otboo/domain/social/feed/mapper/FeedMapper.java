package com.sprint.mission.otboo.domain.social.feed.mapper;

import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedDto;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdSnapshot;
import com.sprint.mission.otboo.domain.social.feed.entity.Feed;
import com.sprint.mission.otboo.domain.weathernotification.weather.dto.PrecipitationDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.dto.TemperatureDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.dto.WeatherSummaryDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FeedMapper {

  public FeedDto toDto(Feed feed, UserSummary author, boolean likedByMe) {
    return new FeedDto(
        feed.getId(),
        feed.getCreatedAt(),
        feed.getUpdatedAt(),
        author,
        toWeatherSummaryDto(feed),
        toOotdDtos(feed.getOotds()),
        feed.getContent(),
        feed.getLikeCount(),
        feed.getCommentCount(),
        likedByMe
    );
  }

  private WeatherSummaryDto toWeatherSummaryDto(Feed feed) {
    if (feed.getSkyStatus() == null) {
      return null;
    }
    return new WeatherSummaryDto(
        feed.getWeatherId(),
        feed.getSkyStatus(),
        new PrecipitationDto(
            feed.getPrecipitationType(),
            feed.getPrecipitationAmount(),
            feed.getPrecipitationProbability()
        ),
        new TemperatureDto(
            feed.getTemperatureCurrent(),
            feed.getTemperatureCompared(),
            feed.getTemperatureMin(),
            feed.getTemperatureMax()
        )
    );
  }

  private List<OotdDto> toOotdDtos(List<OotdSnapshot> snapshots) {
    if (snapshots == null) {
      return List.of();
    }
    return snapshots.stream()
        .map(s -> new OotdDto(s.clothesId(), s.name(), s.imageUrl(), s.type(), s.attributes()))
        .toList();
  }
}