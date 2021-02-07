import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveRedisTest {

    private final RedisClient client = RedisClient.create("redis://localhost");
    private final RedisReactiveCommands<String, String> commands = client.connect().reactive();
    private final RedisCommands<String, String> syncCommands = client.connect().sync();


    @AfterEach
    void tearDown() {
//        syncCommands.flushall();
    }

    @Test
    @DisplayName("리액티브는 응답처리를 별도의 스레드로 작업한")
    void name() throws InterruptedException {
        //given
        final String key = "key";
        final String txValue = "value";
        final CountDownLatch latch = new CountDownLatch(1);
        final Map<String, String> resultMap = new HashMap<>();
        syncCommands.set(key, txValue);

        //when
        commands.get(key).subscribe(value -> {
            System.out.println(Thread.currentThread().getName());
            resultMap.put(key, value);
            latch.countDown();
        });

        //then
        latch.await();
        assertThat(resultMap.get(key)).isEqualTo(txValue);
    }

    @Test
    void name2() throws InterruptedException {
        //given
        final List<String> texts = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            texts.add(i + "");
        }
        final List<String> results = Lists.newArrayList();
        final CountDownLatch latch = new CountDownLatch(texts.size());
        texts.forEach(text -> syncCommands.set(text, text));

        //when
        Flux.just(texts.toArray(new String[0])).
                flatMap(commands::get).
                subscribe(value -> {
                    System.out.println(Thread.currentThread().getName());
                    results.add(value);
                    latch.countDown();
                });
        latch.await();
        //then

        texts.forEach(expected -> assertThat(results).contains(expected));
    }

    @Test
    @DisplayName("lists stream")
    void name3() throws InterruptedException {
        //given
        final List<String> texts = Lists.newArrayList();
        for (int i = 0; i < 1000; i++) {
            String text = Integer.toString(i);
            texts.add(text);
            syncCommands.set(text, text);
        }
        final CountDownLatch latch = new CountDownLatch(texts.size());

        //when
        Flux.just(texts.toArray(new String[0]))
                .flatMap(commands::get)
                .flatMap(value -> {
                    System.out.println(Thread.currentThread().getName());
                    return commands.rpush("result", value);
                })
                .subscribe(value -> latch.countDown());

        //then
        latch.await();
        List<String> results = syncCommands.lrange("result", 0, texts.size());
        assertThat(texts.containsAll(results)).isTrue();
    }

    @Test
    @DisplayName("값이 없으면 무시")
    void name4() throws InterruptedException {
        //given
        final List<String> texts = Lists.newArrayList("Ben", "Michael");
        final List<String> results = Lists.newArrayList();
        final CountDownLatch latch = new CountDownLatch(texts.size());
        texts.forEach(value -> syncCommands.set(value, value));

        //when
        Flux.just("Ben", "Michael", "Mark")
                .flatMap(commands::get)
                .doOnNext(value -> {
                    System.out.println(Thread.currentThread().getName());
                    results.add(value);
                    latch.countDown();
                })
                .subscribe();

        //then
        latch.await();
        assertThat(texts.containsAll(results)).isTrue();
    }

    @Test
    @DisplayName("트랜잭션 적용")
    void name5() throws InterruptedException {
        //given
        syncCommands.flushall();
        CountDownLatch latch = new CountDownLatch(1);
        //when
        commands.multi().subscribe(s -> {
            commands.set("key", "1").doOnNext(System.out::println).subscribe();
            commands.incr("key").doOnNext(System.out::println).subscribe();
            commands.exec().subscribe(s1 -> latch.countDown());
        });

        //then
        latch.await();
        assertThat(syncCommands.get("key")).isEqualTo("2");
    }

    @Test
    @DisplayName("스레드 변경 타이밍 확인")
    void name6() {
        //given
        final List<String> texts = Lists.newArrayList("Ben", "Michael", "Mark");
        texts.forEach(value -> syncCommands.set(value, value));

        //when
        Flux.just("Ben", "Michael", "Mark")
                .filter(s -> {
                    System.out.println(Thread.currentThread().getName());
                    return s.startsWith("M");

                })
                .flatMap(commands::get)
                .flatMap(s -> {
                    System.out.println(Thread.currentThread().getName());
                    return Flux.just(s);
                })
                .subscribe(value -> System.out.println("Got value: " + value));
        //then

    }
}
