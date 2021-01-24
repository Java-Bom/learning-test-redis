import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

class ReactiveTest {
    private final List<String> testData = Arrays.asList("Ben", "Michael", "Mark");
    private ReactiveChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ReactiveChecker(testData);
    }

    @Test
    void name() {
        //given
        Flux.just(testData.toArray(new String[0]))
                .doOnNext(s -> checker.check(s))
                .doOnComplete(() -> assertThat(checker.allMatch()).isTrue())
                .subscribe();
    }


    @Test
    void name2() {
        Flux.just(testData.toArray(new String[0]))
                .doOnNext(s -> checker.check(s))
                .doOnComplete(() -> assertThat(checker.allMatch()).isFalse())
                .take(2)
                .subscribe();

        assertThat(checker.checkResult(testData.get(0))).isTrue();
        assertThat(checker.checkResult(testData.get(1))).isTrue();
        assertThat(checker.checkResult(testData.get(2))).isFalse();
    }

    @Test
    void name3() {
        Flux.just(testData.toArray(new String[0]))
                .doOnNext(s -> {
                    if (s.equals(testData.get(0))) {
                        throw new RuntimeException();
                    }
                })
                .doOnError(s -> checker.check(testData.get(2)))
                .take(2)
                .subscribe();

        assertThat(checker.checkResult(testData.get(2))).isTrue();
    }

    @Test
    void name4() {
        //given
        String last = Flux.just(testData.toArray(new String[0])).last().block();
        assertThat(testData.get(2)).isEqualTo(last);
    }

    @Test
    void name5() {
        //given
        List<String> testData = Flux.just(this.testData.toArray(new String[0])).collectList().block();
        assertThat(this.testData).isEqualTo(testData);
    }

    private static class ReactiveChecker {
        private final Map<String, Integer> testMap;

        public ReactiveChecker(List<String> testData) {
            this.testMap = testData.stream()
                    .collect(toMap(Function.identity(), s -> 0));
        }

        public void check(String s) {
            if (testMap.containsKey(s)) {
                int value = testMap.get(s);
                testMap.put(s, value + 1);
                return;
            }
            testMap.put(s, 0);
        }

        public boolean allMatch() {
            for (Integer value : testMap.values()) {
                if (value != 1) {
                    return false;
                }
            }
            return true;
        }

        public boolean checkResult(String s) {
            return testMap.get(s) == 1;
        }
    }
}
