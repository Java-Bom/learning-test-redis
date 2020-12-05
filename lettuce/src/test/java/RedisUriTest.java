import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 연결 방식
 * <p>
 * redis://[password@]host[:port][/databaseNumber] Plaintext Redis connection
 * <p>
 * rediss://[password@]host[:port][/databaseNumber] SSL Connections Redis connection
 * <p>
 * redis-sentinel://[password@]host[:port][,host2[:port2]][/databaseNumber]#sentinelMasterId for using Redis Sentinel
 * <p>
 * redis-socket:///path/to/socket Unix Domain Sockets connection to Redis
 */
public class RedisUriTest {
    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(connection)) {
            connection.sync().flushall();
            connection.close();
            connection = null;
        }
        if (Objects.nonNull(client)) {
            client.shutdown();
            client = null;
        }
    }

    @DisplayName("String을 이용한 기본 연결 설정 방법")
    @Test
    void connectionTest() {
        client = RedisClient.create("redis://localhost");

        connection = client.connect();
        RedisCommands<String, String> commands = connection.sync();
        commands.set("foo", "foo");

        String value = commands.get("foo");
        commands.del("foo");

        assertThat(value).isEqualTo("foo");
    }

    @DisplayName("존재하지 않는 redis uri에 연결하려는 경우 Exception 발생")
    @Test
    void uriConnectionTest() {
        int wrongPortNumber = 8080;
        client = RedisClient.create(RedisURI.create("redis://localhost", wrongPortNumber));

        assertThatThrownBy(client::connect)
                .isInstanceOf(RedisConnectionException.class)
                .hasMessageContaining("redis://localhost:" + wrongPortNumber);
    }

    @DisplayName("RedisURI Builder 로 연결 설정하기")
    @Test
    void usingRedisURITest() {
        RedisURI redisUri = RedisURI.Builder.redis("localhost")
                .withPassword(new StringBuffer("authentication"))
                .withDatabase(2)
                .build();
        client = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> connect = client.connect();
        RedisCommands<String, String> commands = connect.sync();

        commands.set("foo", "foo");

        String value = commands.get("foo");
        commands.del("foo");

        assertThat(value).isEqualTo("foo");
    }

    @DisplayName("RedisURI Builder 로 Redis client 생성하기")
    @Test
    void usingSSLRedisURITest() {
        RedisURI redisUri = RedisURI.Builder.redis("localhost")
                .withSsl(true)
                .withPassword(new StringBuffer("authentication"))
                .withDatabase(2)
                .build();
        client = RedisClient.create(redisUri);

        assertThat(client).isNotNull();
    }

    @DisplayName("RedisURI 에 String 을 이용해서 client 생성하기")
    @Test
    void stringRedisURI() {
        RedisURI redisUri = RedisURI.create("redis://authentication@localhost/2");
        RedisClient client = RedisClient.create(redisUri);

        assertThat(client).isNotNull();
    }
}
