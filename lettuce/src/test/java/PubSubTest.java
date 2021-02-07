import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

class PubSubTest {
    private final RedisClient client = RedisClient.create("redis://localhost");
    private final StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();


    @Test
    void name() throws InterruptedException {
        //given
        Set<String> messageSets = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(2);
        connection.addListener(new RedisPubSubAdapter<String, String>() {
            public void message(String channel, String message) {
                System.out.println(Thread.currentThread().getName());
                messageSets.add(message);
                latch.countDown();
            }
        });

        RedisPubSubCommands<String, String> sync = connection.sync();
        sync.subscribe("channel");

        //when
        StatefulRedisConnection<String, String> sender = client.connect();

        sender.sync().publish("channel", "Message-1");
        sender.sync().publish("channel", "Message-2");

        latch.await();

        //then
        assertThat(messageSets.size()).isEqualTo(2);
        assertThat(messageSets).contains("Message-1", "Message-2");
    }

    @Test
    void name2() throws InterruptedException {
        //given
        Set<String> messageSets = new HashSet<>();
        CountDownLatch latch = new CountDownLatch(2);
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();

        RedisPubSubReactiveCommands<String, String> reactive = connection.reactive();
        reactive.subscribe("channel").subscribe();

        reactive.observeChannels()
                .doOnNext(patternMessage -> {
                    System.out.println(Thread.currentThread().getName());
                    messageSets.add(patternMessage.getMessage());
                    latch.countDown();
                })
                .subscribe();
        //when
        StatefulRedisConnection<String, String> sender = client.connect();

        sender.sync().publish("channel", "Message-1");
        sender.sync().publish("channel", "Message-2");
        latch.await();

        //then
        assertThat(messageSets.size()).isEqualTo(2);
        assertThat(messageSets).contains("Message-1", "Message-2");
    }
}
