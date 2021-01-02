import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by jyami on 2021/01/02
 */
public class ReactiveStreamTest {
    @Test
    @DisplayName("Consuming Publisher 예제 : emit 된 모든 항목을 subscribe 하고 print 한다.")
    void consumingPublisher() {
        Flux.just("jyami", "java", "javabom").subscribe(new Subscriber<String>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(3);
            }

            @Override
            public void onNext(String s) {
                System.out.println("Hello " + s + "!");
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("Complete consuming!");
            }
        });
    }

    @Test
    @DisplayName("Subscriber<T> 의 더 간단한 구현")
    void subscriberSimpleImpl() {
        Flux.just("Ben", "Michael", "Mark").doOnNext(new Consumer<String>() {
            public void accept(String s) {
                System.out.println("Hello " + s + "!");
            }
        }).doOnComplete(new Runnable() {
            public void run() {
                System.out.println("Completed");
            }
        }).subscribe();
    }

    @Test
    @DisplayName("Subscriber<T> 의 더 간단한 구현 - 람다사용")
    void subscriberSimpleImplWithLambda() {
        Flux.just("Ben", "Michael", "Mark")
                .doOnNext(s -> System.out.println("Hello " + s + "!"))
                .doOnComplete(() -> System.out.println("Completed")).subscribe();
    }

    @Test
    @DisplayName("take() 연산자 : limit와 같이 emit되는 element 조절")
    void takeOperator(){
        Flux.just("Ben", "Michael", "Mark") //
                .doOnNext(s -> System.out.println("Hello " + s + "!"))
                .doOnComplete(() -> System.out.println("Completed"))
                .take(2)
                .subscribe();
    }

    @Test
    @DisplayName("block() 연산자 : async -> sycn")
    void blockOperator() {
        String last = Flux.just("Ben", "Michael", "Mark").last().block();
        System.out.println(last); // Mark
    }

    @Test
    @DisplayName("block() 연산자 : async -> sycn")
    void blockOperatorCollection() {
        List<String> list = Flux.just("Ben", "Michael", "Mark").collectList().block();
        System.out.println(list); // [Ben, Michael, Mark]
    }
}
