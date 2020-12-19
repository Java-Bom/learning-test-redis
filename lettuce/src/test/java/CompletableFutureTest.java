import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CompletableFutureTest {

    @Test
    @DisplayName("thenRun은 complete호출 되면 수행된다.")
    void name() {
        final CompletableFuture<String> future = new CompletableFuture<>();

        AtomicReference<String> validText = new AtomicReference<>("one");
        future.thenRun(() -> validText.set("two"));

        assertThat(future.isDone()).isFalse();
        assertThat(validText.get()).isEqualTo("one");

        future.complete("my value");
        assertThat(future.isDone()).isTrue();
        assertThat(validText.get()).isEqualTo("two");
    }

    @Test
    @DisplayName("thenAccept complete으로 주입받는 value를 소비한다")
    void name2() {
        CompletableFuture<String> future = new CompletableFuture<>();

        AtomicReference<String> validText = new AtomicReference<>("one");
        future.thenAccept(validText::set);

        assertThat(future.isDone()).isFalse();
        assertThat(validText.get()).isEqualTo("one");

        future.complete("two");

        assertThat(future.isDone()).isTrue();
        assertThat(validText.get()).isEqualTo("two");
    }

    @ParameterizedTest
    @CsvSource({"300,400,one", "400,300,two"})
    void name3(int sleep1, int sleep2, String resultText) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            sleep(sleep1);
            return "one";
        });

        CompletableFuture<String> otherFuture = CompletableFuture.supplyAsync(() -> {
            sleep(sleep2);
            return "two";
        });

        AtomicReference<String> validText = new AtomicReference<>("");
        future.acceptEither(otherFuture, validText::set).join();

        assertThat(validText.get()).isEqualTo(resultText);
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.getStackTrace();
        }
    }

}
