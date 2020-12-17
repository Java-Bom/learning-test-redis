import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class RedisAsyncAPITest {
    static RedisClient client = RedisClient.create("redis://localhost");
    static StatefulRedisConnection<String, String> connect = client.connect();
    static RedisAsyncCommands<String, String> commandsAsync = connect.async();

    @AfterAll
    static void clear() {
        commandsAsync.flushall();
        connect.close();
        client.shutdown();
    }

    @DisplayName("CompletableFuture에 complete로 값 세팅하고 get으로 값을 가져온다")
    @Test
    void completableFuture() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = new CompletableFuture<>();

        assertThat(future.isDone()).isFalse();

        future.complete("my value");

        assertThat(future.isDone()).isTrue();
        assertThat(future.get()).isEqualToIgnoringCase("my Value");
    }

    @DisplayName("CompletableFuture에 Listener를 등록해서 사용해본다")
    @Test
    void completableFutureWithListener() {
        CompletableFuture<String> future = new CompletableFuture<>();

        future.thenRun(() -> {
            try {
                System.out.println("Got value: " + future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        assertThat(future.isDone()).isFalse();
        future.complete("my Value");
        assertThat(future.isDone()).isTrue();
    }

    @DisplayName("CompletableFuture에 Consumer 등록")
    @Test
    void completableFutureWithConsumer() {
        CompletableFuture<String> future = new CompletableFuture<>();

        future.thenAccept((value) ->
                {
                    try {
                        System.out.println("Get value: " + future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
        );

        assertThat(future.isDone()).isFalse();
        future.complete("my value");
        assertThat(future.isDone()).isTrue();
    }

    @Test
    void redisGetKey() throws ExecutionException, InterruptedException, TimeoutException {
        RedisFuture<String> value = commandsAsync.get("key");

        assertThat(value.get(1, TimeUnit.MICROSECONDS)).isEqualToIgnoringCase("value");
    }

    @DisplayName("LettuceFutures.awaitAll")
    @Test
    void lettuceFuturesAwaitAll() throws ExecutionException, InterruptedException {
        List<RedisFuture<String>> futures = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            futures.add(commandsAsync.set("key-" + i, "value-" + i)); // set 명렁 이후의 결과값은 OK
        }

        // 여러개 Future 동기화할 때
        // 한개에 대해서는 get을 쓰든 await 을 쓰든
        LettuceFutures.awaitAll(1, TimeUnit.MINUTES, futures.toArray(new RedisFuture[10]));

        assertThat(futures.get(9).isDone()).isTrue();
        assertThat(futures.get(0).get()).isEqualTo("OK");
    }

    @DisplayName("Future chaning")
    @Test
    void futureChaining() {
        CompletableFuture<String> future = new CompletableFuture<>();

        future.thenApply(String::length)
                .thenAccept(length -> System.out.println("Got Value: " + length));

        future.complete("FIVE");
    }

    @DisplayName("exceptionally 사용법")
    @Test
    void exceptionally() throws ExecutionException, InterruptedException {
        CompletableFuture<String> exceptionally = CompletableFuture.supplyAsync(this::failOperation)
                .exceptionally(throwable -> "except");

        assertThat(exceptionally.get()).isEqualTo("except");
    }

    @DisplayName("handle 사용법")
    @Test
    void handle() throws ExecutionException, InterruptedException {
        CompletableFuture<String> handle = CompletableFuture.supplyAsync(this::failOperation)
                .handle((s, t) -> {
                    if (Objects.nonNull(t)) {
                        System.out.println("Exception");
                        return "default value";
                    }
                    return s;
                });

        assertThat(handle.get()).isEqualTo("default value");
    }

    private String failOperation() {
        System.out.println("Fail Operation");
        throw new IllegalStateException();
    }
}
