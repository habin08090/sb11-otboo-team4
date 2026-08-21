# Testcontainers 사용 가이드

> `docs/conventions.md` §9(테스트)와 함께 봅니다. 각 방식을 선택한 근거는 PR/ADR을 참고하세요.

## 1. Postgres

`application-test.yaml`의 JDBC URL이 Testcontainers 전용 스킴(`jdbc:tc:`)이라 별도 설정 없이 자동으로 컨테이너가
연결됩니다. 같은 `jdbc:tc:` URL을 같은 JVM에서 비병렬로 실행하면 드라이버(`ContainerDatabaseDriver`)가 static
캐시로 컨테이너를 재사용합니다 — 이 프로젝트는 이 세 조건(동일 URL/단일 JVM/비병렬)을 항상 만족하므로 이 문서의
나머지 항목들과 달리 신경 쓸 게 없습니다.

## 2. Redis — Spring 컨텍스트 없는 테스트에서 컨테이너 공유하기

Spring 컨텍스트를 안 띄우고 실제 Redis에 직접 붙는 통합 테스트에서 여러 클래스가 컨테이너 하나를 공유하려면, 공유 인터페이스에
`private static` 팩토리 메서드로 컨테이너를 시작시켜 두고 `implements`로 가져다 씁니다.

```java
// global/testcontainers/RedisTestContainerSupport.java
public interface RedisTestContainerSupport {

  GenericContainer<?> REDIS_CONTAINER = createStartedContainer();

  private static GenericContainer<?> createStartedContainer() {
    GenericContainer<?> container =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    container.start();
    return container;
  }
}
```

```java
class UserSessionRedisRegistryTest implements RedisTestContainerSupport {

  @BeforeAll
  static void setUpRedis() {
    connectionFactory = new LettuceConnectionFactory(
        REDIS_CONTAINER.getHost(), REDIS_CONTAINER.getMappedPort(6379));
    // ...
  }
}
```

컨테이너 종료는 Testcontainers의 Ryuk 리퍼가 JVM 종료 시 자동 처리하므로 별도 `stop()` 호출은 필요 없습니다.

**주의 1 — 인터페이스에 `static {}` 블록 사용 불가**: `.start()`처럼 값을 반환하지 않는 호출은 필드 초기화식에 바로 못
넣습니다. 위처럼 `private static` 팩토리 메서드로 감싸서 우회합니다.

**주의 2 — `@Container`/`@Testcontainers` 사용 금지**: 이 확장이 관리하는 static 필드는 해당 테스트 클래스가 끝나면
자동으로 stop됩니다. 여러 클래스가 공유하는 필드에 이 확장을 쓰면, 그중 아무 클래스나 먼저 끝날 때 나머지 클래스가 쓰던
컨테이너까지 죽습니다. 이 패턴은 프레임워크가 생명주기를 아예 소유하지 않도록, 필드 초기화식에서 완전히 수동으로
시작만 시킵니다.

**전제 조건**: 이 패턴은 아래 두 조건이 성립하는 동안만 안전합니다.

- 테스트 실행이 병렬이 아닐 것 (`build.gradle`에 `maxParallelForks`/`parallel` 미설정, `junit-platform.properties`
  없음)
- 어떤 테스트도 다른 테스트의 키를 광범위하게 조회/검증하지 않을 것 (`redisTemplate.keys(...)` 같은 전체 스캔 금지)

`flushAll()`처럼 컨테이너 전체를 비우는 호출은 병렬 실행 시 다른 클래스의 상태를 지울 수 있습니다. 위 두 조건 중 하나라도
깨야 한다면(병렬 실행 도입, 광범위 키 조회 필요 등) 키 접두사 또는 별도 Redis 데이터베이스로 테스트 간 격리를 먼저
추가해야 합니다.

## 3. Elasticsearch

`IntegrationTestSupport`(`@SpringBootTest` 계열) 또는 `ElasticsearchTestContainerSupport`(ES 슬라이스
테스트)를 상속하면 테스트 실행 시 컨테이너가 자동으로 기동됩니다. 로컬 개발용 ES(9200)나 compose
ES(9201)와는 무관하므로, 테스트가 개발 중인 인덱스를 건드리지 않습니다.

```java
class FeedLikeIntegrationTest extends IntegrationTestSupport {
```

```java

@DataElasticsearchTest
class FeedSearchCustomRepositoryImplTest extends ElasticsearchTestContainerSupport {
```

`@SpringBootTest`는 ES 커넥션을 컨텍스트 초기화 시점에 맺으므로, ES를 직접 쓰지 않는 통합 테스트도 컨테이너가 필요합니다.
`@SpringBootTest(classes = {...})`로 범위를 좁혀 ES 자동설정이 올라오지 않는 테스트는 상속하지 않습니다.

**전제 조건 — 이미지 사전 빌드**: Nori 형태소 분석기가 필요해 공식 이미지를 그대로 쓸 수 없습니다. 테스트
실행 전 아래를 한 번 실행해야 합니다. CI는 워크플로에서 같은 명령을 수행합니다.

```bash
docker build -t otboo-es docker/elasticsearch
```

**주의 1 — 인터페이스가 아닌 추상 클래스**: `IntegrationTestSupport`가 이를 상속해,
전체 컨텍스트 테스트와 ES 슬라이스 테스트가 같은 컨테이너와 프로퍼티 등록을 공유하는 계층을 만듭니다.

**주의 2 — `@ServiceConnection` 대신 static 필드**: `@ServiceConnection`으로 빈 등록하면 컨텍스트가 종료될 때 컨테이너도 함께
내려갑니다. 같은 설정의 컨텍스트는 캐시되어 재사용되지만, 이 프로젝트는 `@MockitoBean`/`@TestBean`으로 컨텍스트가 여러 갈래로 갈라져 그만큼 재기동됩니다.
static 필드는 컨텍스트 수명과 무관하게 JVM당 한 번만 뜹니다.

**주의 3 — `asCompatibleSubstituteFor` 필요**: Testcontainers는 이미지 이름으로 ES 여부를 판별합니다.
커스텀 태그(`otboo-es`)를 쓰면 이 선언이 없을 때 기동을 거부합니다.

## 4. Kafka — `@EmbeddedKafka`

`spring-kafka`/`spring-kafka-test` 의존성이 있어야 아래 예제가 동작합니다 — 아직 `build.gradle`에 없으므로
실제 Kafka 클라이언트를 도입하는 시점에 함께 추가합니다.

Docker 컨테이너가 아니라 JVM 내 임베디드 브로커를 씁니다. `@SpringBootTest`처럼 Spring 컨텍스트를 쓰는 테스트
클래스에 `@EmbeddedKafka`를 붙이면 `EmbeddedKafkaBroker` 빈이 등록되고, `spring.kafka.bootstrap-servers`
프로퍼티로 부트스트랩 서버 주소가 채워집니다(Spring Kafka 3.0.10+ 기본값). Spring 컨텍스트 없이 쓰면 standalone
브로커만 생성되고 빈으로 등록되지 않습니다.

```java

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"notification-requested"})
@DirtiesContext
class NotificationKafkaListenerTest {

  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  @Value("${spring.kafka.bootstrap-servers}")
  private String brokerAddresses;

  // KafkaTemplate으로 메시지 발행 → @KafkaListener가 실제로 소비하는지 검증
}
```

임베디드 브로커는 JVM 종료 시 정리되는데, 컨텍스트 캐싱과 얽혀 종료 시점에 레이스 컨디션이 생길 수 있어
`@DirtiesContext`를 함께 붙이는 게 권장됩니다.

## 5. 체크리스트

- [ ] Spring 컨텍스트가 없는 통합 테스트에서 컨테이너를 여러 클래스가 공유해야 하면 2번 패턴(공유 인터페이스 +
  `private static` 팩토리 메서드) 사용
- [ ] `@Container`/`@Testcontainers`와 "여러 클래스 간 공유"를 같이 쓰지 않기 — 클래스 종료 시 자동 stop되므로 공유
  목적엔 안 맞음 (2번 주의 2)
- [ ] 인터페이스 필드 초기화에 `.start()` 같은 부수효과 호출이 필요하면 `static {}` 대신 `private static` 메서드로
  우회 (2번 주의 1)
