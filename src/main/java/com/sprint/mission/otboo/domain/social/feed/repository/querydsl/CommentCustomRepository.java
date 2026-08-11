package com.sprint.mission.otboo.domain.social.feed.repository.querydsl;

import com.sprint.mission.otboo.domain.social.feed.dto.FeedCommentParams;
import com.sprint.mission.otboo.domain.social.feed.entity.Comment;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import java.util.UUID;

public interface CommentCustomRepository {

  CursorPageResponse<Comment> findComments(UUID feedId, FeedCommentParams params);
}