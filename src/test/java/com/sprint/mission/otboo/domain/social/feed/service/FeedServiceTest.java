package com.sprint.mission.otboo.domain.social.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import com.navercorp.fixturemonkey.jakarta.validation.plugin.JakartaValidationPlugin;
import com.sprint.mission.otboo.domain.authuser.user.exception.UserNotFoundException;
import com.sprint.mission.otboo.domain.social.common.dto.UserSummary;
import com.sprint.mission.otboo.domain.social.common.repository.querydsl.UserSummaryQueryRepository;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedCreateRequest;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedDto;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedListParams;
import com.sprint.mission.otboo.domain.social.feed.dto.FeedSortBy;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdDto;
import com.sprint.mission.otboo.domain.social.feed.dto.OotdSnapshot;
import com.sprint.mission.otboo.domain.social.feed.dto.WeatherSnapshot;
import com.sprint.mission.otboo.domain.social.feed.entity.Feed;
import com.sprint.mission.otboo.domain.social.feed.exception.FeedForbiddenException;
import com.sprint.mission.otboo.domain.social.feed.exception.WeatherNotFoundException;
import com.sprint.mission.otboo.domain.social.feed.mapper.FeedMapper;
import com.sprint.mission.otboo.domain.social.feed.repository.FeedRepository;
import com.sprint.mission.otboo.domain.weathernotification.weather.dto.PrecipitationDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.dto.TemperatureDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.dto.WeatherSummaryDto;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.PrecipitationType;
import com.sprint.mission.otboo.domain.weathernotification.weather.entity.enums.SkyStatus;
import com.sprint.mission.otboo.global.dto.CursorPageResponse;
import com.sprint.mission.otboo.global.dto.SortDirection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService")
class FeedServiceTest {

  static final FixtureMonkey fm = FixtureMonkey.builder()
      .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
      .plugin(new JakartaValidationPlugin())
      .build();

  static final WeatherSnapshot DUMMY_SNAPSHOT = new WeatherSnapshot(
      SkyStatus.CLEAR, PrecipitationType.NONE,
      0.0, 0.0, 28.0, 2.0, 16.0, 31.0);

  @InjectMocks
  FeedService feedService;

  @Mock
  FeedRepository feedRepository;

  @Mock
  FeedMapper feedMapper;

  @Mock
  UserSummaryQueryRepository userSummaryQueryRepository;

  @Mock
  WeatherSnapshotProvider weatherSnapshotProvider;

  @Mock
  OotdSnapshotProvider ootdSnapshotProvider;

  @Nested
  @DisplayName("피드 등록")
  class CreateFeed {

    @Test
    @DisplayName("작성자 ID가 인증 사용자와 다르면 FeedForbiddenException을 던진다")
    void 작성자_ID가_인증_사용자와_다르면_FeedForbiddenException을_던진다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", UUID.randomUUID())
          .sample();

      // when & then
      assertThatThrownBy(() -> feedService.create(request, currentUserId))
          .isInstanceOf(FeedForbiddenException.class)
          .satisfies(ex -> {
            FeedForbiddenException fe = (FeedForbiddenException) ex;
            assertThat(fe.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(fe.getDetails())
                .containsEntry("currentUserId", currentUserId)
                .containsKey("requestedAuthorId");
          });
    }

    @Test
    @DisplayName("정상 요청이면 피드를 저장하고 UserSummary를 조회하여 FeedDto를 반환한다")
    void 정상_요청이면_피드를_저장하고_UserSummary를_조회하여_FeedDto를_반환한다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      UserSummary mockAuthor = new UserSummary(currentUserId, "작성자명", "img.png");
      when(weatherSnapshotProvider.readSnapshot(any())).thenReturn(DUMMY_SNAPSHOT);
      given(userSummaryQueryRepository.findByUserId(currentUserId)).willReturn(mockAuthor);
      when(feedRepository.save(any(Feed.class))).thenAnswer(inv -> inv.getArgument(0));

      FeedDto expected = new FeedDto(
          UUID.randomUUID(), null, null, mockAuthor, null, null, request.content(), 0L, 0, false);
      when(feedMapper.toDto(any(Feed.class), eq(mockAuthor), eq(false))).thenReturn(expected);

      // when
      FeedDto result = feedService.create(request, currentUserId);

      // then
      ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
      verify(feedRepository).save(captor.capture());
      verify(userSummaryQueryRepository).findByUserId(currentUserId);

      Feed saved = captor.getValue();
      assertThat(saved.getAuthorId()).isEqualTo(currentUserId);
      assertThat(saved.getWeatherId()).isEqualTo(request.weatherId());
      assertThat(saved.getContent()).isEqualTo(request.content());
      assertThat(saved.getLikeCount()).isZero();
      assertThat(saved.getCommentCount()).isZero();
      assertThat(result).isEqualTo(expected);
      assertThat(result.author()).isEqualTo(mockAuthor);
      assertThat(result.author().name()).isEqualTo("작성자명");
    }

    @Test
    @DisplayName("작성자 정보가 존재하지 않으면 UserNotFoundException을 전파한다")
    void 작성자_정보가_존재하지_않으면_UserNotFoundException을_전파한다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      when(weatherSnapshotProvider.readSnapshot(any())).thenReturn(DUMMY_SNAPSHOT);
      when(ootdSnapshotProvider.readOotds(any(), any())).thenReturn(List.of());
      when(feedRepository.save(any(Feed.class))).thenAnswer(inv -> inv.getArgument(0));
      when(userSummaryQueryRepository.findByUserId(currentUserId))
          .thenThrow(UserNotFoundException.withNone());

      // when & then
      assertThatThrownBy(() -> feedService.create(request, currentUserId))
          .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("정상 등록 시 WeatherSnapshot이 Feed 스냅샷 컬럼에 저장된다")
    void 정상_등록_시_WeatherSnapshot이_Feed_스냅샷_컬럼에_저장된다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      UserSummary author = new UserSummary(currentUserId, "테스터", null);

      when(weatherSnapshotProvider.readSnapshot(request.weatherId())).thenReturn(DUMMY_SNAPSHOT);
      when(userSummaryQueryRepository.findByUserId(currentUserId)).thenReturn(author);
      when(feedRepository.save(any(Feed.class))).thenAnswer(inv -> inv.getArgument(0));
      when(feedMapper.toDto(any(Feed.class), eq(author), eq(false)))
          .thenReturn(new FeedDto(UUID.randomUUID(), null, null, author, null, null,
              request.content(), 0L, 0, false));

      // when
      feedService.create(request, currentUserId);

      // then
      ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
      verify(feedRepository).save(captor.capture());
      verify(weatherSnapshotProvider).readSnapshot(request.weatherId());
      Feed saved = captor.getValue();
      assertThat(saved.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
      assertThat(saved.getPrecipitationType()).isEqualTo(PrecipitationType.NONE);
      assertThat(saved.getTemperatureCurrent()).isEqualTo(28.0);
      assertThat(saved.getTemperatureMin()).isEqualTo(16.0);
      assertThat(saved.getTemperatureMax()).isEqualTo(31.0);
    }

    @Test
    @DisplayName("존재하지 않는 weatherId면 WeatherNotFoundException을 던진다")
    void 존재하지_않는_weatherId면_WeatherNotFoundException을_던진다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      when(weatherSnapshotProvider.readSnapshot(request.weatherId()))
          .thenThrow(WeatherNotFoundException.withId(request.weatherId()));

      // when & then
      assertThatThrownBy(() -> feedService.create(request, currentUserId))
          .isInstanceOf(WeatherNotFoundException.class)
          .satisfies(ex -> {
            WeatherNotFoundException e = (WeatherNotFoundException) ex;
            assertThat(e.getStatus().value()).isEqualTo(400);
            assertThat(e.getDetails()).containsEntry("weatherId",
                request.weatherId());
          });
    }

    @Test
    @DisplayName("정상 등록 시 반환 FeedDto.weather에 WeatherSummaryDto가 채워진다")
    void 정상_등록_시_반환_FeedDto_weather에_WeatherSummaryDto가_채워진다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      UserSummary author = new UserSummary(currentUserId, "테스터", null);

      WeatherSummaryDto expectedWeather = new WeatherSummaryDto(
          request.weatherId(), SkyStatus.CLEAR,
          new PrecipitationDto(PrecipitationType.NONE, 0.0, 0.0),
          new TemperatureDto(28.0, 2.0, 16.0, 31.0));
      FeedDto expected = new FeedDto(
          UUID.randomUUID(), null, null, author, expectedWeather, null,
          request.content(), 0L, 0, false);

      when(weatherSnapshotProvider.readSnapshot(request.weatherId())).thenReturn(DUMMY_SNAPSHOT);
      when(userSummaryQueryRepository.findByUserId(currentUserId)).thenReturn(author);
      when(feedRepository.save(any(Feed.class))).thenAnswer(inv -> inv.getArgument(0));
      when(feedMapper.toDto(any(Feed.class), eq(author), eq(false))).thenReturn(expected);

      // when
      FeedDto result = feedService.create(request, currentUserId);

      // then
      assertThat(result.weather()).isNotNull();
      assertThat(result.weather().skyStatus()).isEqualTo(SkyStatus.CLEAR);
      assertThat(result.weather().temperature().min()).isEqualTo(16.0);
      assertThat(result.weather().temperature().max()).isEqualTo(31.0);
    }

    @Test
    @DisplayName("정상 등록 시 clothesIds로 조회한 OotdSnapshot이 Feed에 저장된다")
    void 정상_등록_시_clothesIds로_조회한_OotdSnapshot이_Feed에_저장된다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      OotdSnapshot ootd1 = fm.giveMeBuilder(OotdSnapshot.class)
          .set("name", "패딩")
          .sample();
      OotdSnapshot ootd2 = fm.giveMeBuilder(OotdSnapshot.class)
          .set("name", "청바지")
          .sample();
      List<OotdSnapshot> ootds = List.of(ootd1, ootd2);

      UserSummary author = new UserSummary(currentUserId, "테스터", null);

      when(weatherSnapshotProvider.readSnapshot(any())).thenReturn(DUMMY_SNAPSHOT);
      when(ootdSnapshotProvider.readOotds(request.clothesIds(), request.authorId()))
          .thenReturn(ootds);
      when(userSummaryQueryRepository.findByUserId(currentUserId)).thenReturn(author);
      when(feedRepository.save(any(Feed.class))).thenAnswer(inv -> inv.getArgument(0));
      when(feedMapper.toDto(any(Feed.class), eq(author), eq(false)))
          .thenReturn(new FeedDto(UUID.randomUUID(), null, null, author, null,
              null, "내용", 0L, 0, false));

      // when
      feedService.create(request, currentUserId);

      // then
      ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
      verify(feedRepository).save(captor.capture());
      verify(ootdSnapshotProvider).readOotds(request.clothesIds(), request.authorId());
      Feed saved = captor.getValue();
      assertThat(saved.getOotds()).hasSize(2);
      assertThat(saved.getOotds().get(0).name()).isEqualTo("패딩");
      assertThat(saved.getOotds().get(1).name()).isEqualTo("청바지");
    }

    @Test
    @DisplayName("정상 등록 시 반환 FeedDto.ootds에 착장 정보가 채워진다")
    void 정상_등록_시_반환_FeedDto_ootds에_착장_정보가_채워진다() {
      // given
      UUID currentUserId = UUID.randomUUID();
      FeedCreateRequest request = fm.giveMeBuilder(FeedCreateRequest.class)
          .set("authorId", currentUserId)
          .sample();

      OotdSnapshot ootdSnapshot = fm.giveMeBuilder(OotdSnapshot.class)
          .set("name", "패딩")
          .sample();
      OotdDto ootdDto = fm.giveMeBuilder(OotdDto.class)
          .set("name", "패딩")
          .sample();
      UserSummary author = new UserSummary(currentUserId, "테스터", null);
      FeedDto expected = new FeedDto(
          UUID.randomUUID(), null, null, author, null, List.of(ootdDto), "내용", 0L, 0, false);

      when(weatherSnapshotProvider.readSnapshot(any())).thenReturn(DUMMY_SNAPSHOT);
      when(ootdSnapshotProvider.readOotds(request.clothesIds(), request.authorId()))
          .thenReturn(List.of(ootdSnapshot));
      when(userSummaryQueryRepository.findByUserId(currentUserId)).thenReturn(author);
      when(feedRepository.save(any(Feed.class))).thenAnswer(inv -> inv.getArgument(0));
      when(feedMapper.toDto(any(Feed.class), eq(author), eq(false))).thenReturn(expected);

      // when
      FeedDto result = feedService.create(request, currentUserId);

      // then
      assertThat(result.ootds()).hasSize(1);
      assertThat(result.ootds().get(0).name()).isEqualTo("패딩");
    }
  }

  @Nested
  @DisplayName("피드 목록 조회")
  class GetFeeds {

    @Test
    @DisplayName("Repository가 준 페이지를 FeedDto로 변환해 반환한다")
    void Repository가_준_페이지를_FeedDto로_변환해_반환한다() {
      // given
      FeedListParams params = new FeedListParams(
          null, null, 2,
          FeedSortBy.CREATED_AT, SortDirection.DESCENDING,
          null, null);

      UUID authorId1 = UUID.randomUUID();
      UUID authorId2 = UUID.randomUUID();
      UserSummary summary1 = new UserSummary(authorId1, "유저1", "img1.png");
      UserSummary summary2 = new UserSummary(authorId2, "유저2", "img2.png");

      Feed feed1 = Feed.create(authorId1, UUID.randomUUID(), "피드1", DUMMY_SNAPSHOT, List.of());
      Feed feed2 = Feed.create(authorId2, UUID.randomUUID(), "피드2", DUMMY_SNAPSHOT, List.of());

      CursorPageResponse<Feed> repoPage = new CursorPageResponse<>(
          List.of(feed1, feed2), "커서값", feed2.getId(), true, 2L,
          "createdAt", SortDirection.DESCENDING);
      when(feedRepository.findFeeds(params)).thenReturn(repoPage);
      given(userSummaryQueryRepository.findByUserIds(anyList()))
          .willReturn(List.of(summary1, summary2));

      FeedDto dto1 = new FeedDto(feed1.getId(), null, null, summary1, null, null, "피드1", 0L, 0,
          false);
      FeedDto dto2 = new FeedDto(feed2.getId(), null, null, summary2, null, null, "피드2", 0L, 0,
          false);
      when(feedMapper.toDto(feed1, summary1, false)).thenReturn(dto1);
      when(feedMapper.toDto(feed2, summary2, false)).thenReturn(dto2);

      // when
      CursorPageResponse<FeedDto> result = feedService.getFeeds(params);

      // then
      assertThat(result.data()).containsExactly(dto1, dto2);
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo("커서값");
      assertThat(result.nextIdAfter()).isEqualTo(feed2.getId());
    }

    @Test
    @DisplayName("Repository가 마지막 페이지를 주면 hasNext false와 null 커서를 그대로 전달한다")
    void Repository가_마지막_페이지를_주면_hasNext_false와_null_커서를_그대로_전달한다() {
      // given
      FeedListParams params = new FeedListParams(
          null, null, 5,
          FeedSortBy.CREATED_AT, SortDirection.DESCENDING,
          null, null);

      UUID authorId1 = UUID.randomUUID();
      UUID authorId2 = UUID.randomUUID();
      UserSummary summary1 = new UserSummary(authorId1, "유저1", "img1.png");
      UserSummary summary2 = new UserSummary(authorId2, "유저2", "img2.png");

      Feed feed1 = Feed.create(authorId1, UUID.randomUUID(), "피드1", DUMMY_SNAPSHOT, List.of());
      Feed feed2 = Feed.create(authorId2, UUID.randomUUID(), "피드2", DUMMY_SNAPSHOT, List.of());

      CursorPageResponse<Feed> repoPage = new CursorPageResponse<>(
          List.of(feed1, feed2), null, null, false, 2L,
          "createdAt", SortDirection.DESCENDING);
      when(feedRepository.findFeeds(params)).thenReturn(repoPage);
      given(userSummaryQueryRepository.findByUserIds(anyList()))
          .willReturn(List.of(summary1, summary2));

      FeedDto dto1 = new FeedDto(feed1.getId(), null, null, summary1, null, null, "피드1", 0L, 0,
          false);
      FeedDto dto2 = new FeedDto(feed2.getId(), null, null, summary2, null, null, "피드2", 0L, 0,
          false);
      when(feedMapper.toDto(feed1, summary1, false)).thenReturn(dto1);
      when(feedMapper.toDto(feed2, summary2, false)).thenReturn(dto2);

      // when
      CursorPageResponse<FeedDto> result = feedService.getFeeds(params);

      // then
      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextIdAfter()).isNull();
      assertThat(result.data()).containsExactly(dto1, dto2);
    }

    @Test
    @DisplayName("여러 author를 findByUserIds 1회로 배치 조회하여 채운다")
    void 여러_author를_findByUserIds_1회로_배치_조회하여_채운다() {
      // given
      FeedListParams params = new FeedListParams(
          null, null, 2,
          FeedSortBy.CREATED_AT, SortDirection.DESCENDING,
          null, null);

      UUID authorId1 = UUID.randomUUID();
      UUID authorId2 = UUID.randomUUID();

      Feed feed1 = Feed.create(authorId1, UUID.randomUUID(), "피드1", DUMMY_SNAPSHOT, List.of());
      Feed feed2 = Feed.create(authorId2, UUID.randomUUID(), "피드2", DUMMY_SNAPSHOT, List.of());

      CursorPageResponse<Feed> repoPage = new CursorPageResponse<>(
          List.of(feed1, feed2), null, null, false, 2L,
          "createdAt", SortDirection.DESCENDING);
      when(feedRepository.findFeeds(params)).thenReturn(repoPage);

      UserSummary summary1 = new UserSummary(authorId1, "유저1", "img1.png");
      UserSummary summary2 = new UserSummary(authorId2, "유저2", "img2.png");
      given(userSummaryQueryRepository.findByUserIds(anyList()))
          .willReturn(List.of(summary1, summary2));

      FeedDto dto1 = new FeedDto(feed1.getId(), null, null, summary1, null, null, "피드1", 0L, 0,
          false);
      FeedDto dto2 = new FeedDto(feed2.getId(), null, null, summary2, null, null, "피드2", 0L, 0,
          false);
      when(feedMapper.toDto(feed1, summary1, false)).thenReturn(dto1);
      when(feedMapper.toDto(feed2, summary2, false)).thenReturn(dto2);

      // when
      CursorPageResponse<FeedDto> result = feedService.getFeeds(params);

      // then
      verify(userSummaryQueryRepository).findByUserIds(anyList());
      assertThat(result.data()).containsExactly(dto1, dto2);
    }
  }
}