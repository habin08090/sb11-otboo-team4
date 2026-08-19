package com.sprint.mission.otboo.domain.social.feed.repository.elasticsearch;

import com.sprint.mission.otboo.domain.social.feed.dto.FeedListParams;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedSearchResult;

public interface FeedSearchCustomRepository {

  FeedSearchResult search(FeedListParams params);
}
