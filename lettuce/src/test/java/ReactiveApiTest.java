import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class ReactiveApiTest {

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

    @DisplayName("subscribe 로 consumer 를 등록해놓고 비동기로 실행할 수 있다." +
            "비동기 실행은 lettuce의 이벤트 루프를 이용한다.")
    @Test
    void reactiveGetTest1() throws InterruptedException {
        RedisReactiveCommands<String, String> commands = connection.reactive();
        Set<String> threadNames = new HashSet<>();
        threadNames.add(Thread.currentThread().getName());

        commands.set("key", "redis").block();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        commands
                .get("key")
                .subscribe(value -> {
                    threadNames.add(Thread.currentThread().getName());
                    System.out.println(Thread.currentThread().getName());
                    System.out.println("value: " + value);
                    countDownLatch.countDown();
                });

        countDownLatch.await();
        assertThat(threadNames).hasSize(2);
    }

    @DisplayName("subscribe 로 consumer 를 등록해놓고 비동기로 실행할 수 있다." +
            "get 한 값이 null이면 실행되지 않는다.")
    @Test
    void reactiveGetTest2() throws InterruptedException {
        RedisReactiveCommands<String, String> commands = connection.reactive();
        Set<String> threadNames = new HashSet<>();
        threadNames.add(Thread.currentThread().getName());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        commands
                .get("key")
                .subscribe(value -> {
                    threadNames.add(Thread.currentThread().getName());
                    System.out.println(Thread.currentThread().getName());
                    System.out.println("value: " + value);
                    countDownLatch.countDown();
                });

        boolean await = countDownLatch.await(100, TimeUnit.MILLISECONDS);

        assertThat(await).isFalse();
        assertThat(threadNames).hasSize(1);
    }

    @DisplayName("Flux 를 이용해서 여러개를 비동기적으로 실행할 수 있다.")
    @Test
    void reactiveGetTest3() throws InterruptedException {
        RedisReactiveCommands<String, String> commands = connection.reactive();

        commands.set("key1", "hello").block();
        commands.set("key2", "redis").block();

        CountDownLatch countDownLatch = new CountDownLatch(2);
        Flux.just("key1", "key2")
                .flatMap(commands::get)
                .subscribe(value -> {
                    System.out.println(value);
                    countDownLatch.countDown();
                });

        countDownLatch.await();
    }

    @DisplayName("reduce 를 이용해서 합쳐서 발행할수도 있다.")
    @Test
    public void fluxReduceTest1() throws InterruptedException {
        RedisReactiveCommands<String, String> commands = connection.reactive();

        //sadd : set add
        commands.sadd("Ben", "hello").block();
        commands.sadd("Michael", "redis").block();
        commands.sadd("Mark", "javabom").block();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Set<Long> results = new HashSet<>();
        Flux.just("Ben", "Michael", "Mark")
                .flatMap(commands::scard)
                .reduce(Long::sum)
                .subscribe(result -> {
                            results.add(result);
                            System.out.println("Number of elements in sets: " + result);
                            countDownLatch.countDown();
                        }
                );

        countDownLatch.await();

        assertThat(results).hasSize(1);
        assertThat(results).contains(3L);
    }

    @DisplayName("value 가 null 인 값을 응답으로 받으면 무시하고 실행하지 않는다.")
    @Test
    void absentValueTest1() throws InterruptedException {
        RedisReactiveCommands<String, String> commands = connection.reactive();
        commands.set("Ben", "hello").block();
        commands.set("Michael", "redis").block();

        Set<String> values = new HashSet<>();
        CountDownLatch countDownLatch = new CountDownLatch(3);

        Flux.just("Ben", "Michael", "Mark")
                .flatMap(commands::get)
                .doOnNext(value -> {
                    System.out.println(value);
                    values.add(value);
                    countDownLatch.countDown();
                })
                .subscribe();

        boolean await = countDownLatch.await(100, TimeUnit.MILLISECONDS);

        assertThat(await).isFalse();
        assertThat(countDownLatch.getCount()).isEqualTo(1);

        assertThat(values).hasSize(2);
        assertThat(values).contains("hello", "redis");
    }

    @DisplayName("defaultIfEmpty 는 빈 시퀀스의 경우 기본 값을 반환해준다. ")
    @Test
    void absentValueTest2() throws InterruptedException {
        RedisReactiveCommands<String, String> commands = connection.reactive();

        Set<String> values = new HashSet<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Flux.just("")
                .flatMap(commands::get)
                .defaultIfEmpty("default")
                .doOnNext(value -> {
                    System.out.println("doOnNext: "+value);
                    values.add(value);
                    countDownLatch.countDown();
                })
                .subscribe(value -> {
                    System.out.println("subscribe: "+value);
                });

        countDownLatch.await();

        assertThat(countDownLatch.getCount()).isEqualTo(0);

        assertThat(values).hasSize(1);
        assertThat(values).contains("default");
    }
}
