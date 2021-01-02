import io.lettuce.core.RedisClient;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * Created by jyami on 2021/01/02
 */
public class ReactiveStreamLettuceTest {

    private RedisStringReactiveCommands<String, String> commands;

    @BeforeEach
    void setUp() {
        RedisClient client = RedisClient.create("redis://localhost");
        commands = client.connect().reactive();
    }

    @Test
    @DisplayName("lettuce publisher를 사용하고, 여기서 get 연산자를 통해 redis의 key에 따른 value를 가져올 수 있다. ")
    void LettucePublisher() {
        commands.get("key")
                .subscribe(System.out::println);
    }

    @Test
    @DisplayName("lettuce publisher는 여러개의 키들을 asynchronously(비동기적으로) 로드하는데 사용될 수 있다.")
    void name() {
        Flux.just("jyami", "javabom", "java").
                flatMap(commands::get)
                .subscribe(value -> System.out.println("Got value: " + value));
    }
}
