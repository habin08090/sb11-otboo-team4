package com.sprint.mission.otboo.domain.social.feed.repository.querydsl.impl;

import static com.sprint.mission.otboo.domain.social.feed.entity.QComment.comment;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedCommentParams;
import com.sprint.mission.otboo.domain.social.feed.entity.Comment;
import com.sprint.mission.otboo.domain.social.feed.repository.querydsl.CommentCustomRepository;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import com.sprint.mission.otboo.global.dto.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class CommentCustomRepositoryImpl implements CommentCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<Comment> findComments(UUID feedId, FeedCommentParams params) {
    List<Comment> raw = fetchComments(feedId, params);

    boolean hasNext = raw.size() > params.limit();
    List<Comment> page = hasNext ? raw.subList(0, params.limit()) : raw;

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !page.isEmpty()) {
      Comment last = page.get(page.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    return new CursorPageResponse<>(page, nextCursor, nextIdAfter, hasNext,
        countComments(feedId), "createdAt", SortDirection.DESCENDING);
  }

  private List<Comment> fetchComments(UUID feedId, FeedCommentParams params) {
    return queryFactory
        .selectFrom(comment)
        .where(
            comment.feedId.eq(feedId),
            cursorCondition(params.cursor(), params.idAfter())
        )
        .orderBy(comment.createdAt.desc(), comment.id.desc())
        .limit(params.limit() + 1L)
        .fetch();
  }

  private long countComments(UUID feedId) {
    return Optional.ofNullable(
        queryFactory.select(comment.count())
            .from(comment)
            .where(comment.feedId.eq(feedId))
            .fetchOne()
    ).orElse(0L);
  }

  private BooleanExpression cursorCondition(String cursor, UUID idAfter) {
    if (!StringUtils.hasText(cursor)) {
      return null;
    }
    Instant instant = Instant.parse(cursor);
    return comment.createdAt.lt(instant)
        .or(comment.createdAt.eq(instant).and(comment.id.lt(idAfter)));
  }
}