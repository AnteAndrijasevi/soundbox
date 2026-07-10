package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Follow;
import hr.andrijasevic.soundbox.domain.FollowId;
import hr.andrijasevic.soundbox.domain.Notification;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.event.ListenLoggedEvent;
import hr.andrijasevic.soundbox.repository.FollowRepository;
import hr.andrijasevic.soundbox.repository.NotificationRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User follower(long id, String username) {
        return User.builder().id(id).username(username).email(username + "@example.com").passwordHash("x").build();
    }

    private Follow followOf(User followerUser, long actorId) {
        return Follow.builder()
                .id(FollowId.builder().followerId(followerUser.getId()).followingId(actorId).build())
                .follower(followerUser)
                .build();
    }

    @Test
    void createFollowerNotifications_fansOutOneNotificationPerFollower() {
        ListenLoggedEvent event = new ListenLoggedEvent(1L, "vera", "mbid-1", "OK Computer");
        User ante = follower(2L, "ante");
        User milo = follower(3L, "milo");
        when(followRepository.findByIdFollowingId(1L))
                .thenReturn(List.of(followOf(ante, 1L), followOf(milo, 1L)));

        int count = notificationService.createFollowerNotifications(event);

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).allSatisfy(n -> {
            assertThat(n.getActorUsername()).isEqualTo("vera");
            assertThat(n.getAlbumTitle()).isEqualTo("OK Computer");
            assertThat(n.getMessage()).isEqualTo("vera logged OK Computer");
            assertThat(n.isRead()).isFalse();
        });
        assertThat(saved).extracting(n -> n.getUser().getDisplayUsername())
                .containsExactlyInAnyOrder("ante", "milo");
    }

    @Test
    void createFollowerNotifications_noFollowers_savesEmpty() {
        ListenLoggedEvent event = new ListenLoggedEvent(1L, "vera", "mbid-1", "OK Computer");
        when(followRepository.findByIdFollowingId(1L)).thenReturn(List.of());

        assertThat(notificationService.createFollowerNotifications(event)).isZero();
        verify(notificationRepository).saveAll(List.of());
    }
}
