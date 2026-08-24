package com.sprint.mission.otboo.domain.social.common.repository.querydsl.impl;

import static com.sprint.mission.otboo.domain.authuser.user.entity.QProfile.profile;
import static com.sprint.mission.otboo.domain.authuser.user.entity.QUser.user;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.otboo.domain.authuser.user.exception.UserNotFoundException;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.common.repository.querydsl.UserSummaryQueryRepository;
import com.sprint.mission.otboo.global.file.util.FileUrlResolver;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserSummaryQueryRepositoryImpl implements UserSummaryQueryRepository {

  private final JPAQueryFactory queryFactory;
  private final FileUrlResolver fileUrlResolver;

  public UserSummary findByUserId(UUID userId) {
    UserSummary result = queryFactory
        .select(Projections.constructor(UserSummary.class,
            user.id, user.name, profile.profileImageUrl))
        .from(user)
        .leftJoin(profile).on(profile.user.id.eq(user.id))
        .where(user.id.eq(userId))
        .fetchOne();

    if (result == null) {
      throw UserNotFoundException.withNone();
    }
    return withResolvedImageUrl(result);
  }

  @Override
  public boolean existsByUserId(UUID userId) {
    Integer fetchOne = queryFactory
        .selectOne()
        .from(user)
        .where(user.id.eq(userId))
        .fetchFirst();

    return fetchOne != null;
  }

  public List<UserSummary> findByUserIds(Collection<UUID> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return List.of();
    }

    return queryFactory
        .select(Projections.constructor(UserSummary.class,
            user.id, user.name, profile.profileImageUrl))
        .from(user)
        .leftJoin(profile).on(profile.user.id.eq(user.id))
        .where(user.id.in(userIds))
        .fetch()
        .stream()
        .map(this::withResolvedImageUrl)
        .toList();
  }

  // DB에는 저장 키만 들어 있어 응답에 그대로 내보내면 브라우저가 이미지를 못 찾는다.
  // UserSummary를 쓰는 Follow·Feed·Comment·DM이 각자 변환하지 않도록 이 레포에서 완성한다.
  private UserSummary withResolvedImageUrl(UserSummary summary) {
    return new UserSummary(
        summary.userId(),
        summary.name(),
        fileUrlResolver.resolve(summary.profileImageUrl()));
  }
}
