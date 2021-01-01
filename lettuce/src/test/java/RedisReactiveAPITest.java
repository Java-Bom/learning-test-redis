import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class RedisReactiveAPITest {

    @DisplayName("Publisher Flux 사용하기 - Subscriber")
    @ValueSource(ints = {1, 2, 3})
    @ParameterizedTest
    void flux_subscribe(int num) {
        CountDownLatch latch = new CountDownLatch(num);
        Flux.just("Ben", "Michael", "Mark").subscribe(new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription s) { // subscribe 가 호출될 때 실행되는 메서드
                s.request(num);
            }

            @Override
            public void onNext(String s) { // 다음 Signal 이 있을 때마다.
                System.out.println("Hello, " + s + "!");
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                System.out.println("Complete");
            }
        });
        assertThat(latch.getCount()).isEqualTo(0);
    }

    @DisplayName("Flux 사용하기 - Flux API")
    @Test
    void flux_consumer() {
        Flux.just("Ben", "Michael", "Mark")
                .doOnNext(s -> System.out.println("Hello " + s + "!"))
                .doOnComplete(() -> System.out.println("Completed"))
                .take(3) // Subscription.request
                .subscribe();
    }

    @DisplayName("Publisher Flux 사용하기 - Subscriber, 에러핸들링")
    @Test
    void flux_subscribe_error() {
        assertThatThrownBy(() -> Flux.just("Ben", "Michael", "Mark")
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onSubscribe(Subscription s) { // subscribe 가 호출될 때 실행되는 메서드
                        s.request(3);
                        throw new RuntimeException("Error Handling");
                    }

                    @Override
                    public void onNext(String s) { // 다음 Signal 이 있을 때마다.
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("OnError!");
                        throw new RuntimeException("OnError");
                    }

                    @Override
                    public void onComplete() {
                    }
                })).isInstanceOf(RuntimeException.class)
                .hasMessage("OnError");
    }
}
