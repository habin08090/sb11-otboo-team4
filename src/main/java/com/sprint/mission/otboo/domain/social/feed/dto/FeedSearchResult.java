package com.sprint.mission.otboo.domain.social.feed.dto;

import java.util.List;
import java.util.UUID;

/**
 * Elasticsearch 검색 결과. ES는 어떤 피드가 어떤 순서인지만 답하고, 실제 데이터는 DB에서 조회한다.
 */
public record FeedSearchResult(
    List<UUID> feedIds,
    long totalCount,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext
) {

}
