package com.sprint.mission.otboo.domain.social.feed.repository;

import com.sprint.mission.otboo.domain.social.feed.entity.Comment;
import com.sprint.mission.otboo.domain.social.feed.repository.querydsl.CommentCustomRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentCustomRepository {

}