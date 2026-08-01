package com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.querydsl.impl;

import static com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.QClothes.clothes;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesListParams;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.dto.ClothesType;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity.Clothes;
import com.sprint.mission.otboo.domain.clothesrecommend.clothes.repository.querydsl.ClothesCustomRepository;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import com.sprint.mission.otboo.global.dto.SortDirection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClothesCustomRepositoryImpl implements ClothesCustomRepository {

  private final JPAQueryFactory queryFactory;

  @Override
  public CursorPageResponse<Clothes> findClothes(ClothesListParams params) {
    List<Clothes> raw = queryFactory
        .selectFrom(clothes)
        .where(
            clothes.ownerId.eq(params.ownerId()),
            isNotDeleted(),
            eqType(params.typeEqual()),
            cursorCondition(params)
        )
        .orderBy(
            new OrderSpecifier<>(Order.DESC, clothes.createdAt),
            new OrderSpecifier<>(Order.DESC, clothes.id)
        )
        .limit(params.limit() + 1L)
        .fetch();

    boolean hasNext = raw.size() > params.limit();
    List<Clothes> data = hasNext ? raw.subList(0, params.limit()) : raw;

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !data.isEmpty()) {
      Clothes last = data.get(data.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    long totalCount = countClothes(params);

    return new CursorPageResponse<>(
        data, nextCursor, nextIdAfter, hasNext, totalCount,
        "createdAt", SortDirection.DESCENDING);
  }

  private long countClothes(ClothesListParams params) {
    return Optional.ofNullable(
        queryFactory
            .select(clothes.count())
            .from(clothes)
            .where(
                clothes.ownerId.eq(params.ownerId()),
                isNotDeleted(),
                eqType(params.typeEqual())
            )
            .fetchOne()
    ).orElse(0L);
  }

  private BooleanExpression isNotDeleted() {
    return clothes.softDeletable.deletedAt.isNull();
  }

  private BooleanExpression eqType(ClothesType type) {
    return type == null ? null : clothes.type.eq(type);
  }

  private BooleanExpression cursorCondition(ClothesListParams params) {
    if (params.cursor() == null) {
      return null;
    }
    Instant cursorTime = Instant.parse(params.cursor());
    UUID cursorId = params.idAfter();
    return clothes.createdAt.lt(cursorTime)
        .or(clothes.createdAt.eq(cursorTime).and(clothes.id.lt(cursorId)));
  }
}