package com.sprint.mission.otboo.domain.clothesrecommend.recommendation.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 추천 LLM 결과 캐시의 적중 여부를 집계한다.
 *
 * <p>히트율은 {@code hit / (hit + miss + error)}로 읽는다. 조회 실패({@code error})도 결과적으로는 LLM을 다시 호출하므로
 * 미적중이지만, 캐시가 비어서 못 맞춘 것과 Redis가 아파서 못 맞춘 것은 대응이 달라 따로 센다.
 */
@Component
public class RecommendationCacheMetrics {

  public static final String HIT = "recommendation.cache.hit";
  public static final String MISS = "recommendation.cache.miss";
  public static final String ERROR = "recommendation.cache.error";

  private final Counter hitCounter;
  private final Counter missCounter;
  private final Counter errorCounter;

  public RecommendationCacheMetrics(MeterRegistry registry) {
    this.hitCounter = Counter.builder(HIT)
        .description("추천 LLM 결과 캐시 적중 횟수")
        .register(registry);
    this.missCounter = Counter.builder(MISS)
        .description("추천 LLM 결과 캐시 미적중 횟수")
        .register(registry);
    this.errorCounter = Counter.builder(ERROR)
        .description("추천 LLM 결과 캐시 조회·저장 실패 횟수")
        .register(registry);
  }

  public void countHit() {
    hitCounter.increment();
  }

  public void countMiss() {
    missCounter.increment();
  }

  public void countError() {
    errorCounter.increment();
  }
}
