package com.redis.lettuce;

import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.internal.Futures;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class AsynchronousTest {
    private RedisClient client;
    private RedisAsyncCommands<String, String> commands;

    @BeforeEach
    void setUp() {
        client = RedisClient.create("redis://localhost:6379");
        commands = client.connect().async();
    }

    @AfterEach
    void tearDown() {
        client.shutdown();
    }

    @DisplayName("get 메소드로 결과 가져오기")
    @Test
    void consumingFutureGet() throws ExecutionException, InterruptedException {
        RedisFuture<String> future = commands.get("foo");
        String value = future.get();
        while (!future.isDone()) {

        }
        assertThat(value).isEqualTo("hi");
    }

    @DisplayName("TimeOut 시간 지정")
    @Test
    void blockingSynchronization() throws InterruptedException, ExecutionException, TimeoutException {
        RedisFuture<String> future = commands.get("foo");
        String value = future.get(1, TimeUnit.SECONDS);
        while (!future.isDone()) {

        }
        assertThat(value).isEqualTo("hi");
    }

    @DisplayName("Future 객체가 지정한 일을 수행 후 인자로 넘겨진 일을 수행한다.")
    @Test
    void consumerListener() {
        RedisFuture<String> future = commands.get("foo");
        future.thenAccept((value) -> {
            try {
                Thread.sleep(2000);
                assertThat(value).isEqualTo("hi");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("consumer listener");
    }

    @DisplayName("작업과 실행을 분리")
    @Test
    void usingExecutor() {
        Executor executor = command -> {
            System.out.println("running executor");
            command.run();
        };

        RedisFuture<String> future = commands.get("foo");

        future.thenAcceptAsync((value) -> {
            System.out.println("running command");
            assertThat(value).isEqualTo("hi");
        }, executor);
    }

    @DisplayName("Futures의 awaitAll 메소드로 모든 작업이 수행될 때 까지 대기한다.")
    @Test
    void useAwaitAll() throws ExecutionException, InterruptedException {
        List<RedisFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            RedisFuture<String> future = commands.set("key-" + i, "value-" + i);
            future.thenAccept(System.out::println);

            futures.add(future);
        }
        Futures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[futures.size()]));

        System.out.println("set complete!");

        for (int i = 0; i < 10; i++) {
            assertThat(commands.get("key-" + i).get()).isEqualTo("value-" + i);
        }

        //추가한 key 제거
        List<RedisFuture<Long>> removes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RedisFuture<Long> future = commands.del("key-" + i);
            removes.add(future);
        }
        Futures.awaitAll(1, TimeUnit.MINUTES, removes.toArray(new RedisFuture[removes.size()]));

        System.out.println("remove complete!");
    }


    //Future chaining
    @DisplayName("thenApply 메소드로 future로 지정한 task의 결과 값을 변환 시킬 수 있다.")
    @Test
    void thenApply() {
        RedisFuture<String> future = commands.get("foo");

        future.thenApply(String::length)
                .thenAccept((length) -> assertThat(length).isEqualTo(2));
    }

    /**
     * @throws InterruptedException 기존에 사용하는 command.get에 강제로 sleep을 걸어서
     *                              1번 DB의 foo 키의 값을 가져오는 의도인데 sleep을 어떻게 걸지..?
     */
/*    @DisplayName("acceptEither로 둘중 먼저 응답을 받는 결과를 사용한다.")
    @Test
    void acceptEither() throws InterruptedException {
        RedisClient replica = RedisClient.create("redis//localhost:6370/database=1");
        RedisAsyncCommands<String, String> replicaCommands = replica.connect().async();
        replicaCommands.set("foo", "bye").await(1, TimeUnit.MINUTES);

        RedisFuture<String> future = commands.get("foo");

        future.acceptEither(replicaCommands.get("foo"), (value) -> assertThat(value).isEqualTo("bye"));

        replica.shutdown();
    }*/
    @DisplayName("handle메소드로 error handling 할 경우엔 exception 여부에 상관없이 메소드 수행")
    @Test
    void errorHandling() {
        RedisFuture<String> future = commands.get("foo");

        future.handle((s, throwable) -> {
            System.out.println("무조건 호출");
            if (throwable != null) {
                return "default";
            }

            return s;
        });
    }

    @DisplayName("exceptionally 메소드로 error handling 할 경우엔 exception 발생했을 때만 메소드 수행")
    @Test
    void errorHandlingWithExceptionally() {
        RedisFuture<String> future = commands.get("foo");

        future.exceptionally(throwable -> {
            System.out.println("예외가 발생했을 때 호출");
            if (throwable instanceof IllegalStateException) {
                return "default value";
            }

            return "other default value";
        });
    }
}
