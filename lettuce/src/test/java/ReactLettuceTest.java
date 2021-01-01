import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import org.junit.jupiter.api.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ReactLettuceTest {
    private final RedisClient redisClient = RedisClient.create("redis://localhost");
    private RedisStringReactiveCommands<String, String> commands = redisClient.connect().reactive();

    @BeforeEach
    void setUp() {
        commands.append("key", "value").block();
    }

    @AfterEach
    void clear() {
        redisClient.connect().sync().flushall();
        redisClient.connect().close();
        redisClient.shutdown();
    }

    @DisplayName("get이후 subscribe하면 async하게 동작한다")
    @Test
    void get_subscribe() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executors = Executors.newSingleThreadExecutor((a) -> new Thread("custom"));
        commands.get("key")
//                .subscribeOn(Schedulers.fromExecutor(executors))
                .subscribe(val -> {
                    System.out.println(val);
                    System.out.println(getCurrentThreadName());
                    latch.countDown();
                });
        System.out.println("Main"); // TODO: 이거 왜 key 출력까지 기다리는지 아시는분
        latch.await();
    }

    private String getCurrentThreadName() {
        return Thread.currentThread().getName();
    }
}
