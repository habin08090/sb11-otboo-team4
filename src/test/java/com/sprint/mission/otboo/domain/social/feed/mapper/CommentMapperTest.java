package com.sprint.mission.otboo.domain.social.feed.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.FieldReflectionArbitraryIntrospector;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.feed.dto.CommentDto;
import com.sprint.mission.otboo.domain.social.feed.entity.Comment;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CommentMapper")
class CommentMapperTest {

  static final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(FieldReflectionArbitraryIntrospector.INSTANCE)
      .build();

  CommentMapper commentMapper = new CommentMapper();

  @Nested
  @DisplayName("toDto 변환")
  class ToDto {

    @Test
    @DisplayName("Comment와 author로 CommentDto를 반환한다")
    void Comment와_author로_CommentDto를_반환한다() {
      // given
      UUID feedId = UUID.randomUUID();
      UUID authorId = UUID.randomUUID();
      Comment comment = fm.giveMeBuilder(Comment.class)
          .set("feedId", feedId)
          .set("authorId", authorId)
          .set("content", "댓글 내용")
          .sample();
      UserSummary author = new UserSummary(authorId, "경신", null);

      // when
      CommentDto dto = commentMapper.toDto(comment, author);

      // then
      assertThat(dto.id()).isEqualTo(comment.getId());
      assertThat(dto.createdAt()).isEqualTo(comment.getCreatedAt());
      assertThat(dto.feedId()).isEqualTo(feedId);
      assertThat(dto.author()).isEqualTo(author);
      assertThat(dto.content()).isEqualTo("댓글 내용");
    }
  }
}