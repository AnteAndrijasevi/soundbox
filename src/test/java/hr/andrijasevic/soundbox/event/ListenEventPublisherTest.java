package hr.andrijasevic.soundbox.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListenEventPublisherTest {

    @Mock
    private KafkaTemplate<String, ListenLoggedEvent> kafkaTemplate;

    @InjectMocks
    private ListenEventPublisher publisher;

    @Test
    void publish_sendsToTopicKeyedByUserId() {
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());
        ListenLoggedEvent event = new ListenLoggedEvent(42L, "vera", "mbid-1", "OK Computer");

        publisher.publish(event);

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ListenLoggedEvent> value = ArgumentCaptor.forClass(ListenLoggedEvent.class);
        verify(kafkaTemplate).send(topic.capture(), key.capture(), value.capture());

        assertThat(topic.getValue()).isEqualTo(ListenLoggedEvent.TOPIC);
        assertThat(key.getValue()).isEqualTo("42"); // keyed by user id
        assertThat(value.getValue()).isEqualTo(event);
    }
}
