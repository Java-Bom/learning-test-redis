import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.async.RedisStringAsyncCommands;
import io.lettuce.core.internal.Futures;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

public class RedisReactiveApiTest {

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;

    @BeforeEach
    void setUp() {
        client = RedisClient.create("redis://localhost");
        connection = client.connect();
    }

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(connection)) {
            connection.sync().flushall();
            connection.close();
            connection = null;
        }
        if (Objects.nonNull(client)) {
            client.shutdown();
            client = null;
        }
    }

    @DisplayName("async 테스트")
    @Test
    void redisAsyncCommandTest1() throws ExecutionException, InterruptedException {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();

        //when
        RedisFuture<String> future = commands.get("key");

        String value = future.get();

        //then
        assertThat(value).isNull();
    }


    @DisplayName("thenAccpet를 이용해서 받은 후의 동작을 Consumer 콜백으로 정의할 수 있다.")
    @Test
    void redisAsyncCommandTest2() throws InterruptedException {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();

        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        RedisFuture<String> future = commands.get("key");
        future.thenAccept((value) -> {
            System.out.println(value);
            countDownLatch.countDown();
        });

        //then
        countDownLatch.await();
        assertThat(countDownLatch.getCount()).isEqualTo(0);
    }

    @DisplayName("thenAccpetAsync를 이용해서 받은 후의 동작을 지정한 Executor에서 Consumer 콜백으로 정의할 수 있다.")
    @Test
    void redisAsyncCommandTest3() throws InterruptedException {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();
        Set<String> threadNames = new HashSet<>();
        threadNames.add(Thread.currentThread().getName());
        Executor executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());


        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        RedisFuture<String> future = commands.get("key");
        future.thenAcceptAsync((value) -> {
            threadNames.add(Thread.currentThread().getName());
            System.out.println(value);
            countDownLatch.countDown();
        }, executor);

        //then
        countDownLatch.await();
        assertThat(countDownLatch.getCount()).isEqualTo(0);
        assertThat(threadNames).hasSize(2);
        System.out.println(threadNames);
    }

    @DisplayName("LettuceFutures.awaitAll 로 Collection의 futrue가 끝날때 까지 대기할 수 있다." +
            "하지만 LettuceFutures 가 Deprecated 되었으니 사용하지 말자.")
    @Test
    void awaitAllTest1() {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();
        List<RedisFuture<String>> futures = new ArrayList<>();

        //when
        for (int i = 0; i < 10; i++) {
            futures.add(commands.set("key-" + i, "value-" + i));
        }
        LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[0]));

        //then
        assertThat(futures).filteredOn(Future::isDone).hasSize(10);
    }

    @DisplayName("LettuceFutures 대신 Futures 를 사용하자.")
    @Test
    void awaitAllTest2() {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();
        List<RedisFuture<String>> futures = new ArrayList<>();

        //when
        for (int i = 0; i < 10; i++) {
            futures.add(commands.set("key-" + i, "value-" + i));
        }
        Futures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[0]));

        //then
        assertThat(futures).filteredOn(Future::isDone).hasSize(10);
    }

    @DisplayName("future의 체이닝을 통해 연쇄적인 작업을 행할수 있다.")
    @Test
    void futureChainingTest() {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();
        String key = "hello";
        String value = "lettuce";
        RedisFuture<String> setFuture = commands.set(key, value);
        Set<String> chainResult = new HashSet<>();

        Futures.await(1, TimeUnit.SECONDS, setFuture);

        //when
        RedisFuture<String> helloFuture = commands.get("hello");
        helloFuture
                .thenApply(arg -> key + arg)
                .thenAccept(chainResult::add);

        //then
        assertThat(chainResult).containsExactly(key + value);
    }

    @DisplayName("thenRun 은 다루는 데이터가 중요하지 않을때 이용하도록 하자." +
            "Blocking IO가 일어나는 작업이라면 커스텀한 Executor를 정의해서 사용하도록 하자.")
    @Test
    void thenRunTest1() throws InterruptedException {
        //given
        RedisAsyncCommands<String, String> commands = connection.async();
        Set<String> threadNames = new HashSet<>();
        threadNames.add(Thread.currentThread().getName());

        //when
        CountDownLatch countDownLatch = new CountDownLatch(1);
        RedisFuture<String> future = commands.get("foo");

        future.thenRun(() -> {
            threadNames.add(Thread.currentThread().getName());
            countDownLatch.countDown();
        });

        //then
        countDownLatch.await();
        assertThat(threadNames).hasSize(2);
        System.out.println(threadNames);
    }

    @Disabled
    @DisplayName("either 류 메서드는 먼저 행동이 일어나는쪽의 결과를 가져다 사용한다.")
    @Test
    void name() {
        RedisClient upstreamClient = RedisClient.create("redis://localhost");
        StatefulRedisConnection<String, String> upstreamConnection = upstreamClient.connect();
        RedisClient replicaClient = RedisClient.create("redis://localhost");
        StatefulRedisConnection<String, String> replicaConnection = replicaClient.connect();

        RedisStringAsyncCommands<String, String> upstream = upstreamConnection.async();
        RedisStringAsyncCommands<String, String> replica = replicaConnection.async();
        String key = "hello";
        RedisFuture<String> future = upstream.get(key);
        future.acceptEither(replica.get(key), value -> System.out.println("먼저 값을 가져온 것을 사용한다. : " + value));

        upstreamConnection.sync().flushall();
        upstreamConnection.close();
        upstreamClient.shutdown();

        replicaConnection.sync().flushall();
        replicaConnection.close();
        replicaClient.shutdown();
    }
}
