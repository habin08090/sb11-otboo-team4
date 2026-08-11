package com.sprint.mission.otboo.domain.social.follow.repository;

import com.sprint.mission.otboo.domain.social.follow.entity.Follow;
import com.sprint.mission.otboo.domain.social.follow.repository.querydsl.FollowCustomRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowCustomRepository {

  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  long countByFolloweeId(UUID followeeId);

  long countByFollowerId(UUID followerId);

  @Query("select f.followerId from Follow f where f.followeeId = :followeeId")
  List<UUID> findFollowerIds(@Param("followeeId") UUID followeeId);

}