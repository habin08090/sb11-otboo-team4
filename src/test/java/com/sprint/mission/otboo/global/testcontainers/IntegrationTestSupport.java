package com.sprint.mission.otboo.global.testcontainers;

/**
 * 전체 컨텍스트를 띄우는 통합 테스트의 공통 베이스.
 *
 * <p>{@code @SpringBootTest}는 ES 커넥션을 포함한 전체 빈을 초기화하므로, ES를 직접 쓰지 않는
 * 테스트도 컨테이너가 필요하다. 앞으로 Kafka 등 다른 인프라가 추가되면 이 클래스에 컨테이너와 {@code @DynamicPropertySource}를 함께 얹는다.
 *
 * <p>ES만 필요한 슬라이스 테스트({@code @DataElasticsearchTest})는
 * {@link ElasticsearchTestContainerSupport}를 직접 상속한다.
 */
public abstract class IntegrationTestSupport extends ElasticsearchTestContainerSupport {

}
