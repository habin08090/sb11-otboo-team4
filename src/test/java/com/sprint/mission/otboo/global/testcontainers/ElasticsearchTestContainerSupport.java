package com.sprint.mission.otboo.global.testcontainers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * ES 테스트용 컨테이너
 *
 * <p>{@code RedisTestContainerSupport}와 달리 인터페이스가 아닌 추상 클래스다.
 * {@link IntegrationTestSupport}가 이를 상속해, 전체 컨텍스트 테스트와 ES 슬라이스 테스트가 같은 컨테이너와 프로퍼티 등록을 공유하는 계층을
 * 만든다.
 *
 * <p>컨테이너를 static 필드로 두어 JVM당 한 번만 기동한다. {@code @ServiceConnection}으로 빈 등록하면
 * 컨텍스트가 종료될 때 컨테이너도 함께 내려가는데, 이 프로젝트는 {@code @MockitoBean}/{@code @TestBean}으로 컨텍스트가 여러 갈래로 갈라져 그만큼
 * 재기동된다. static 필드는 컨텍스트 수명과 무관하게 유지된다.
 *
 * <p>Nori 플러그인이 필요해 {@code docker/elasticsearch} 이미지를 쓴다. 테스트 전에 아래를 한 번 실행해야 한다.
 * <pre>docker build -t otboo-es docker/elasticsearch</pre>
 */
public abstract class ElasticsearchTestContainerSupport {

  private static final DockerImageName IMAGE = DockerImageName
      .parse("otboo-es:latest")
      .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

  protected static final ElasticsearchContainer ES_CONTAINER = createStartedContainer();

  private static ElasticsearchContainer createStartedContainer() {
    ElasticsearchContainer container = new ElasticsearchContainer(IMAGE)
        .withEnv("xpack.security.enabled", "false")
        .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
    container.start();
    return container;
  }

  @DynamicPropertySource
  static void registerElasticsearchProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.elasticsearch.uris",
        () -> "http://" + ES_CONTAINER.getHttpHostAddress());
  }
}
