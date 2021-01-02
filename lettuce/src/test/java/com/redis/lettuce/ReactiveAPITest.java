package com.redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReactiveAPITest {

    @DisplayName("Flux를 이용한 publisher" +
            "한 subscriber가 stream 에서 3개를 요청한다" +
            "onNext 메소드를 호출하면서 data를 읽어 지정한 일을 수행한다" +
            "더 이상 읽을 데이터가 없다면 onComplete 메소드를 호출한다.")
    @ParameterizedTest
    @CsvSource({"2, 2 ,false", "3, 4,true"})
    void basicConsumingPublisher(int request, int expectedSize, boolean expectedResult) {
        List<String> readDatas = new ArrayList<>();
        Flux.just("Ben", "Michael", "Mark").subscribe(new Subscriber<String>() {
            @Override
            public void onSubscribe(final Subscription s) {
                s.request(request);
            }

            @Override
            public void onNext(final String s) {
                readDatas.add(s);
            }

            @Override
            public void onError(final Throwable t) {
            }

            @Override
            public void onComplete() {
                readDatas.add("completed");
            }
        });

        assertThat(readDatas.size()).isEqualTo(expectedSize);
        assertThat(readDatas.contains("completed")).isEqualTo(expectedResult);
    }

    @DisplayName("SubScriber를 구현하지 않고 onNext와 onComplete 구현")
    @Test
    void consumingPublisherWithConsumerMethod() {
        List<String> readDatas = new ArrayList<>();
        Flux.just("Ben", "Michael", "Mark")
                .doOnNext(readDatas::add)
                .doOnComplete(() -> readDatas.add("completed"))
                .subscribe();

        assertThat(readDatas.size()).isEqualTo(4);
        assertThat(readDatas.contains("completed")).isEqualTo(true);
    }

    @DisplayName("take 메소드로 읽을 데이터의 갯수를 조절")
    @Test
    void consumingPublisherWithLimitReadData() {
        List<String> readDatas = new ArrayList<>();
        Flux.just("Ben", "Michael", "Mark")
                .doOnNext(readDatas::add)
                .doOnComplete(() -> {
                    readDatas.add("completed");
                    System.out.println("completed");
                })
                .take(2)
                .subscribe();

        assertThat(readDatas.size()).isEqualTo(2);
        assertThat(readDatas.contains("completed")).isEqualTo(false);
    }

    @Test
    void fluxWithLettuce() {
        RedisClient client = RedisClient.create("redis://localhost:6379");
        RedisStringReactiveCommands<String, String> commands = client.connect().reactive();

        List<String> values = new ArrayList<>();

        Flux.just("ben", "michael", "mark")
                .flatMap(commands::get)
                .subscribe(values::add);

        assertThat(values).contains("imMichael", "imBen", "imMark");
    }
}
