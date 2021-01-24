import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FluxTest {

    @DisplayName("flux를 이용한 publisher 테스트")
    @Test
    void publisherTest() {
        Flux.just("Ben", "Michael", "Mark").subscribe(new BaseSubscriber<String>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                super.hookOnSubscribe(subscription);
                super.request(1);
            }

            @Override
            protected void hookOnNext(String value) {
                super.hookOnNext(value);
                System.out.println("Hello " + value + "!");
                super.request(1);
            }

            @Override
            protected void hookOnComplete() {
                super.hookOnComplete();
                System.out.println("Completed");
            }

            @Override
            protected void hookOnError(Throwable throwable) {
                super.hookOnError(throwable);
            }
        });
    }

    @DisplayName("순서를 알아보자.")
    @Test
    void hookTest() {
        Flux.just("Ben", "Michael", "Mark")
                .doOnNext((value) -> System.out.println("1.doOnNext: " + value))
                .doOnComplete(() -> System.out.println("3.doOnComplete"))
                .subscribe(
                        (value) -> System.out.println("2.consume: " + value),
                        System.out::println,
                        () -> System.out.println("4.complete")
                );
    }

    @DisplayName("람다를 이용해서 체이닝 형태로 구현 가능하다.")
    @Test
    void publisherLambdasTest() {
        Set<String> complete = new HashSet<>();

        Flux.just("Ben", "Michael", "Mark")
                .doOnNext(s -> System.out.println("Hello " + s + "!"))
                .doOnComplete(() -> {
                            complete.add("complete");
                            System.out.println("Completed");
                        }
                )
                .subscribe();

        assertThat(complete).hasSize(1);
    }

    @DisplayName("take 를 이용해서 limit 효과를 내서 특정갯수만 처리할 수 있다." +
            "특정 갯수를 처리한 뒤 암묵적으로 뒤의 subscription 은 취소 처리한다." +
            "전부 처리하지 않으면 onComplete 는 불리지 않는다.")
    @Test
    void takeTest() {
        Set<String> complete = new HashSet<>();
        Flux.just("Ben", "Michael", "Mark")
                .doOnNext(s -> System.out.println("Hello " + s + "!"))
                .doOnComplete(() -> complete.add("complete"))
                .take(2)
                .subscribe();

        assertThat(complete).hasSize(0);
    }

    @DisplayName("마지막 구독 정보만 가져온다.")
    @Test
    void lastTest() {
        String last = Flux.just("Ben", "Michael", "Mark").last().block();
        System.out.println(last);
    }

    @DisplayName("처리된 정보를 리스트로 묶는다.")
    @Test
    void collectListTest() {
        System.out.println(Flux.just("Ben", "Michael", "Mark").collectList().block());
    }

    @DisplayName("groupby 를 이용해서 분류 가능")
    @Test
    public void fluxGroupByTest() {
        Flux.just("Ben", "Michael", "Mark")
                .groupBy(key -> key.substring(0, 1))
                .subscribe(
                        groupedFlux -> {
                            groupedFlux.collectList().subscribe(list -> {
                                System.out.println("First character: " + groupedFlux.key() + ", elements: " + list);
                            });
                        }
                );
    }

}
