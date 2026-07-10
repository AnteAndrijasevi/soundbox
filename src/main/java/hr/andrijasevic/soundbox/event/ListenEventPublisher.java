package hr.andrijasevic.soundbox.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link ListenLoggedEvent}s to Kafka. Fire-and-forget: a broker problem is
 * logged but never fails the user's request (logging a listen must always succeed).
 * Disabled when {@code app.events.enabled=false} (e.g. in tests).
 */
@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true", matchIfMissing = true)
public class ListenEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ListenEventPublisher.class);

    private final KafkaTemplate<String, ListenLoggedEvent> kafkaTemplate;

    public ListenEventPublisher(KafkaTemplate<String, ListenLoggedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ListenLoggedEvent event) {
        // key by user so one user's events stay ordered on a partition
        kafkaTemplate.send(ListenLoggedEvent.TOPIC, String.valueOf(event.userId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to publish ListenLoggedEvent for user {}: {}",
                                event.userId(), ex.toString());
                    } else {
                        log.debug("Published ListenLoggedEvent for user {} ({})",
                                event.userId(), event.albumTitle());
                    }
                });
    }
}
