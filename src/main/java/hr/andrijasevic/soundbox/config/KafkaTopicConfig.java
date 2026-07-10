package hr.andrijasevic.soundbox.config;

import hr.andrijasevic.soundbox.event.ListenLoggedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public NewTopic listenLoggedTopic() {
        return TopicBuilder.name(ListenLoggedEvent.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
