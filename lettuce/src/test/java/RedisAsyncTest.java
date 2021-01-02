import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RedisAsyncTest {
    private static final RedisClient CLIENT = RedisClient.create("redis://localhost");
    private static final StatefulRedisConnection<String, String> CONNECTION = CLIENT.connect();
    private final RedisReactiveCommands<String, String> reactiveCommands = CONNECTION.reactive();
    private final RedisAsyncCommands<String, String> commands = CONNECTION.async();

    private final String key = "key";
    private final String value = "value";

    @AfterAll
    static void afterAll() {
        CONNECTION.close();
        CLIENT.shutdown();
    }

    @BeforeEach
    void setUp() {
        CONNECTION.async().set(key, value);

    }

    @AfterEach
    void tearDown() {
        commands.flushall();
    }

    @Test
    @DisplayName("complet이 호출될때까지 결과를 가져올 수 없다")
    void name() {
        RedisFuture<String> future = commands.get(key);
        future.thenAcceptAsync(value -> {
            System.out.println(Thread.currentThread().getName());
        });

        future.exceptionally(throwable -> {
            if (throwable instanceof IllegalStateException) {
                return "default value";
            }

            return "other default value";
        });
    }

    @Test
    @DisplayName("thenAccept test")
    void name2() throws InterruptedException {
        //given
        RedisFuture<String> future = commands.get("key");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> validText = new AtomicReference<>("one");
        AtomicReference<String> threadName = new AtomicReference<>("thread");

        //when
        future.thenAccept(value -> {
            validText.set(value);
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        latch.await();

        //then
        assertThat(validText.get()).isEqualTo(value);
//        assertThat(threadName.get()).contains("lettuce-nioEventLoop-");
    }

    @Test
    @DisplayName("thenAcceptAsync test")
    void name3() throws InterruptedException {
        //given
        RedisFuture<String> future = commands.get("key");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> validText = new AtomicReference<>("one");
        AtomicReference<String> threadName = new AtomicReference<>("thread");

        //when
        future.thenAcceptAsync(value -> {
            validText.set(value);
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        latch.await();

        //then
        assertThat(validText.get()).isEqualTo(value);
        assertThat(threadName.get()).contains("ForkJoinPool.commonPool-worker-");
    }

    @CsvSource({"MINUTES,true", "MILLISECONDS,false"})
    @ParameterizedTest
    void name4(TimeUnit timeUnit, boolean result) {
        //given
        List<RedisFuture<String>> futures = new ArrayList<>();

        //when
        for (int i = 0; i < 10; i++) {
            futures.add(commands.set("key-" + i, "value-" + i));
        }

        //then
        assertThat(LettuceFutures.awaitAll(1, timeUnit, futures.toArray(new RedisFuture[0]))).isEqualTo(result);

    }

    @Test
    void name5() throws InterruptedException {
        RedisFuture<String> future = commands.get("key");
        AtomicInteger value = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);

        future.thenApply(String::length)
                .thenAccept(integer -> {
                    value.set(integer);
                    latch.countDown();
                });

        latch.await();
        assertThat(value.get()).isEqualTo(5);
    }

    @Test
    void name6() {
        assertThatThrownBy(() -> {
            RedisFuture<String> future = commands.get(key);
            future.get(1, TimeUnit.MICROSECONDS);
        }).isInstanceOf(TimeoutException.class);
    }

    @Test
    void name7() throws InterruptedException {
        RedisFuture<String> future = commands.get(key);
        assertThat(future.await(1, TimeUnit.MICROSECONDS)).isFalse();
    }

    @Test
    void name8() throws InterruptedException {
        //given
        AtomicReference<String> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        RedisFuture<String> future = commands.get(key);

        future.handle((value, throwable) -> {
            if (throwable != null) {
                return "fail";
            }
            return "success";
        })
                .thenAccept(value -> {
                    result.set(value);
                    latch.countDown();
                });
        latch.await();

        assertThat(result.get()).isEqualTo("success");
    }
}
