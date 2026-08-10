package com.sprint.mission.otboo.domain.social.feed.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sprint.mission.otboo.domain.social.feed.entity.Comment;
import com.sprint.mission.otboo.global.config.JpaConfig;
import com.sprint.mission.otboo.global.config.QuerydslConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QuerydslConfig.class})
@DisplayName("CommentRepository")
class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("댓글을 저장하면 id가 부여되고 필드가 보존된다")
    void 댓글을_저장하면_id가_부여되고_필드가_보존된다() {
      // given
      UUID feedId = UUID.randomUUID();
      UUID authorId = UUID.randomUUID();
      Comment comment = Comment.create(feedId, authorId, "댓글 내용");

      // when
      Comment saved = commentRepository.saveAndFlush(comment);

      // then
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getFeedId()).isEqualTo(feedId);
      assertThat(saved.getAuthorId()).isEqualTo(authorId);
      assertThat(saved.getContent()).isEqualTo("댓글 내용");
      assertThat(saved.getCreatedAt()).isNotNull();
    }
  }
}