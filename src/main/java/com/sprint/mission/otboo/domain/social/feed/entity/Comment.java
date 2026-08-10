package com.sprint.mission.otboo.domain.social.feed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "comments")
@Entity
public class Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", columnDefinition = "uuid", nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "feed_id", nullable = false)
  private UUID feedId;

  @Column(name = "author_id", nullable = false)
  private UUID authorId;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  private Comment(UUID feedId, UUID authorId, String content) {
    this.feedId = feedId;
    this.authorId = authorId;
    this.content = content;
  }

  public static Comment create(UUID feedId, UUID authorId, String content) {
    return new Comment(feedId, authorId, content);
  }
}