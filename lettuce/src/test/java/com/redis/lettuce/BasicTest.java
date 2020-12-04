package com.redis.lettuce;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BasicTest {
    private RedisClient client;

    @DisplayName("팩토리 메소드로 RedisURI 생성")
    @Test
    void useFactoryMethod() {
        RedisURI redisURI = RedisURI.create("redis://8455@localhost:6379");

        getValueTest(redisURI);

        client.shutdown();
    }

    @DisplayName("builder로 RedisURI 생성")
    @Test
    void userBuilder() {
        RedisURI redisURI = RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .withPassword("8455")
                .build();

        getValueTest(redisURI);

        client.shutdown();
    }

    @DisplayName("setter로 RedisURI 호스트, 포트번호 설정")
    @Test
    void setValue() {
        RedisURI redisURI = new RedisURI();
        redisURI.setHost("localhost");
        redisURI.setPort(6379);
        redisURI.setPassword("8455");

        getValueTest(redisURI);

        client.shutdown();
    }

    @DisplayName("password 없을때 접근시 Exception throw")
    @Test
    void wrongPasswordThrowExceptions() {
        RedisURI redisURI = RedisURI.create("redis://localhost:6379");

        RedisClient redisClient = RedisClient.create(redisURI);

        assertThatThrownBy(() -> redisClient.connect())
                .isInstanceOf(RedisConnectionException.class);
    }

    @DisplayName("Redis URI 패턴이 다를때 exception Throw")
    @ParameterizedTest
    @ValueSource(strings = {"redis://8455@localhos:6379", "redis://8455@localhos:6378"})
    void wrongURIThrowException(String uri) {
        RedisURI redisURI = RedisURI.create(uri);
        RedisClient redisClient = RedisClient.create(redisURI);

        assertThatThrownBy(() -> redisClient.connect())
                .isInstanceOf(RedisConnectionException.class);
    }

    /*
    @Ignore
    @DisplayName("Timeout 시간 후 접근시 exception throw")
    @Test
    void timeoutThrowException() throws InterruptedException {
        RedisURI redisURI = RedisURI.builder()
                .withHost("localhost")
                .withPort(6379)
                .withTimeout(Duration.ofSeconds(2))
                .build();

        RedisClient client = RedisClient.create(redisURI);

        StatefulRedisConnection<String, String> connection = client.connect();

        RedisCommands<String, String> commands = connection.sync();

        Thread.sleep(4000);

        assertThatThrownBy(() -> commands.get("foo"))
                .isInstanceOf(RedisCommandExecutionException.class);

        client.shutdown();
    }
    */

    @DisplayName("없는 key는 null return")
    @Test
    void wrongKeyReturnNull() {
        RedisURI redisURI = RedisURI.create("redis://8455@localhost:6379");

        RedisClient redisClient = RedisClient.create(redisURI);

        StatefulRedisConnection<String, String> connect = redisClient.connect();

        RedisCommands<String, String> commands = connect.sync();

        String value = commands.get("notFoo");

        assertThat(value).isNullOrEmpty();
    }

    private void getValueTest(RedisURI redisURI) {
        client = RedisClient.create(redisURI);

        StatefulRedisConnection<String, String> connection = client.connect();

        RedisCommands<String, String> commands = connection.sync();

//        commands.auth("8455");

        String value = commands.get("foo");

        assertThat(value).isEqualTo("hi");

        client.shutdown();
    }
}
