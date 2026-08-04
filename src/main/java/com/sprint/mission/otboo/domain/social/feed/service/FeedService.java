package com.sprint.mission.otboo.domain.social.feed.service;

import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.common.repository.querydsl.UserSummaryQueryRepository;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedCreateRequest;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedDto;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedListParams;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdSnapshot;
import com.sprint.mission.otboo.domain.social.feed.dto.WeatherSnapshot;
import com.sprint.mission.otboo.domain.social.feed.entity.Feed;
import com.sprint.mission.otboo.domain.social.feed.exception.FeedForbiddenException;
import com.sprint.mission.otboo.domain.social.feed.mapper.FeedMapper;
import com.sprint.mission.otboo.domain.social.feed.repository.FeedRepository;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class FeedService {

  private final FeedRepository feedRepository;
  private final UserSummaryQueryRepository userSummaryQueryRepository;
  private final FeedMapper feedMapper;
  private final WeatherSnapshotProvider weatherSnapshotProvider;
  private final OotdSnapshotProvider ootdSnapshotProvider;

  @Transactional
  public FeedDto create(FeedCreateRequest request, UUID currentUserId) {
    if (!request.authorId().equals(currentUserId)) {
      throw FeedForbiddenException.authorMismatch(currentUserId, request.authorId());
    }

    WeatherSnapshot weatherSnapshot = weatherSnapshotProvider.readSnapshot(request.weatherId());
    List<OotdSnapshot> ootdSnapshots = ootdSnapshotProvider.readOotds(request.clothesIds(),
        request.authorId());

    Feed feed = feedRepository.save(
        Feed.create(request.authorId(), request.weatherId(), request.content(), weatherSnapshot,
            ootdSnapshots));
    log.info("피드 등록 완료: feedId={}", feed.getId());

    UserSummary author = userSummaryQueryRepository.findByUserId(feed.getAuthorId());
    return feedMapper.toDto(feed, author, false);
  }

  public CursorPageResponse<FeedDto> getFeeds(FeedListParams params) {
    CursorPageResponse<Feed> page = feedRepository.findFeeds(params);

    List<Feed> feeds = page.data();

    List<UUID> authorIds = feeds.stream()
        .map(Feed::getAuthorId)
        .distinct()
        .toList();

    Map<UUID, UserSummary> authorMap = userSummaryQueryRepository.findByUserIds(authorIds).stream()
        .collect(Collectors.toMap(UserSummary::userId, Function.identity(),
            (existing, replacement) -> existing));

    List<FeedDto> data = feeds.stream()
        .map(feed -> {
          UserSummary author = authorMap.get(feed.getAuthorId());
          return feedMapper.toDto(feed, author, false);
        })
        .toList();

    return new CursorPageResponse<>(
        data, page.nextCursor(), page.nextIdAfter(), page.hasNext(),
        page.totalCount(), page.sortBy(), page.sortDirection());
  }
}