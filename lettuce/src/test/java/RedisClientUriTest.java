import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisClientUriTest {

    private RedisClient client;
    private StatefulRedisConnection<String, String> connection;

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.sync().flushall();
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
        this.connection = null;
        this.client = null;
    }

    @Test
    @DisplayName("redisUri를 통해 레디스 연결이 가능하다.")
    void name() {
        //given
        RedisURI redisUri = RedisURI.Builder.redis("localhost")
                .withPassword("root") // 패스워드
                .withDatabase(2) // db 저장
                .build();


        client = RedisClient.create(redisUri);
        connection = client.connect();

        String uuid = UUID.randomUUID().toString();

        //when
        RedisCommands<String, String> commands = connection.sync();
        commands.set("foo", uuid);
        String value = commands.get("foo");

        //then
        assertThat(value).isEqualTo(uuid);
    }

    @Test
    @DisplayName("문자열 형태로 레디스 연결")
    void name2() {
        //given
        RedisURI redisUri = RedisURI.create("redis://root@localhost/2");
        client = RedisClient.create(redisUri);

        StatefulRedisConnection<String, String> connection = client.connect();

        String uuid = UUID.randomUUID().toString();

        //when
        RedisCommands<String, String> commands = connection.sync();
        commands.set("foo", uuid);
        String value = commands.get("foo");

        //then
        assertThat(value).isEqualTo(uuid);
    }

    @Test
    @DisplayName("잘못된 비밀번호는 연결 실패한다.")
    void name3() {
        //given
        RedisURI redisUri = RedisURI.create("redis://root-1@localhost/2");
        client = RedisClient.create(redisUri);

        //when
        //then
        assertThatThrownBy(client::connect)
                .isInstanceOf(RedisException.class);

    }

}
