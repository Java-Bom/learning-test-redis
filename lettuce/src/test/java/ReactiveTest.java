import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

class ReactiveTest {

    @Test
    void name() {
        //given

        Flux<String> flux = Flux.just("Ben", "Michael", "Mark");

        //when
        flux.subscribe(new Subscriber<String>() {
            private Subscription subscription;

            public void onSubscribe(Subscription s) {
                this.subscription = s;
                this.subscription.request(1);
            }

            public void onNext(String s) {
                System.out.println("Hello " + s + "!");
                subscription.request(1);
            }

            public void onError(Throwable t) {
                System.out.println("Hello ");
            }

            public void onComplete() {
                System.out.println("Completed");
            }
        });

        //then
    }


    @CsvSource({"2,2,0", "4,3,1"})
    @ParameterizedTest
    void name2(int take, int eventCount, int result) {
        //given
        Set<String> events = new HashSet<>();
        Set<String> results = new HashSet<>();

        //when
        Flux.just("Ben", "Michael", "Mark") //
                .doOnNext(events::add)
                .doOnComplete(() -> results.add("Completed"))
                .take(take)
                .subscribe();

        //then
        assertThat(events.size()).isEqualTo(eventCount);
        assertThat(results.size()).isEqualTo(result);
    }

    @Test
    void name3() {
        //given
        //when
        String last = Flux.just("Ben", "Michael", "Mark")
                .last()
                .block();
        System.out.println(last);

        //then

    }


    @Test
    void name4() {
        //given

        //when
        List<String> list = Flux.just("Ben", "Michael", "Mark").collectList().block();
        System.out.println(list);
        //then

    }

    @Test
    void name5() throws InterruptedException {
        //given
//        EmitterProcessor<Long> data = EmitterProcessor.create(1);
//        data.subscribe(t -> System.out.println(t));
//        FluxSink<Long> sink = data.sink();
//        sink.next(10L);
//        sink.next(11L);
//        sink.next(12L);
//        data.subscribe(t -> System.out.println(t));
//        sink.next(13L);
//        sink.next(14L);
//        sink.next(15L);
//
        EmitterProcessor<String> emitter = EmitterProcessor.create();
        FluxSink<String> sink = emitter.sink();
        emitter.publishOn(Schedulers.single())
                .map(String::toUpperCase)
//                .filter(s -> s.startsWith("HELLO"))
                .delayElements(Duration.of(1000, MILLIS))
                .subscribe(System.out::println);

        sink.next("Hello World!");
        sink.next("Goodbye World");
        sink.next("Again");
        Thread.sleep(3000);
    }
}
