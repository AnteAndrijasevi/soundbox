package hr.andrijasevic.soundbox.event;

import hr.andrijasevic.soundbox.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link ListenLoggedEvent}s and fans out notifications to the actor's
 * followers. Decoupled from the write path — logging a listen returns immediately;
 * this runs asynchronously off the Kafka topic.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = ListenLoggedEvent.TOPIC, groupId = "soundbox-notifications")
    public void onListenLogged(ListenLoggedEvent event) {
        int fanned = notificationService.createFollowerNotifications(event);
        log.debug("ListenLoggedEvent for user {} → {} follower notification(s)", event.userId(), fanned);
    }
}
