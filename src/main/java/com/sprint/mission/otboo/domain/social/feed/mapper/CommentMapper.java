package com.sprint.mission.otboo.domain.social.feed.mapper;

import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.feed.dto.CommentDto;
import com.sprint.mission.otboo.domain.social.feed.entity.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

  public CommentDto toDto(Comment comment, UserSummary author) {
    return new CommentDto(
        comment.getId(),
        comment.getCreatedAt(),
        comment.getFeedId(),
        author,
        comment.getContent()
    );
  }
}