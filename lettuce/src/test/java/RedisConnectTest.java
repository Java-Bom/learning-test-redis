import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class RedisConnectTest {

    /**
     * 로컬 redis 실행 방법: redis-server
     */
    @DisplayName("Redis 연결 테스트")
    @Test
    void basicConnect() {
        RedisClient client = RedisClient.create("redis://localhost");

        StatefulRedisConnection<String, String> connect = client.connect();

        RedisCommands<String, String> commands = connect.sync(); // 현재 커넥션에서 수행할 수 있는 synchronous API 반환

        String pong = commands.ping();

        /**
         * 테스트 실패 케이스를 위해 tearDown으로 빼두는게 좋다.
         */
        connect.close();
        client.shutdown();

        assertAll(
                () -> assertThat(pong).isEqualToIgnoringCase("pong"),
                () -> assertThat(connect.isOpen()).isFalse()
        );
    }

    @DisplayName("RedisClient With Builder")
    @Test
    void builder(){
        RedisClient client = RedisClient.create(RedisURI.Builder.redis("localhost").build());
    }

}
