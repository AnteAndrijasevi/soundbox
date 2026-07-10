package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Notification;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.NotificationDto;
import hr.andrijasevic.soundbox.event.ListenLoggedEvent;
import hr.andrijasevic.soundbox.exception.ResourceNotFoundException;
import hr.andrijasevic.soundbox.repository.FollowRepository;
import hr.andrijasevic.soundbox.repository.NotificationRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            FollowRepository followRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    /**
     * Fans a listen-logged event out to the actor's followers, one notification each.
     * Invoked from the Kafka consumer.
     */
    @Transactional
    public int createFollowerNotifications(ListenLoggedEvent event) {
        List<Notification> notifications = followRepository.findByIdFollowingId(event.userId()).stream()
                .map(follow -> Notification.builder()
                        .user(follow.getFollower())
                        .actorUsername(event.username())
                        .albumMbid(event.albumMbid())
                        .albumTitle(event.albumTitle())
                        .message(event.username() + " logged " + event.albumTitle())
                        .read(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(String email, Pageable pageable) {
        User user = requireUser(email);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public int unreadCount(String email) {
        return notificationRepository.countByUserIdAndReadFalse(requireUser(email).getId());
    }

    @Transactional
    public void markAllRead(String email) {
        User user = requireUser(email);
        notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged())
                .forEach(n -> n.setRead(true));
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationDto mapToDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getActorUsername(),
                n.getAlbumMbid(),
                n.getAlbumTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
