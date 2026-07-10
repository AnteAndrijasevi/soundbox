package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.NotificationDto;
import hr.andrijasevic.soundbox.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notifications", description = "Follower notifications built from listen-logged events")
@RestController
@RequestMapping("/api/users/me/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(currentEmail(), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(currentEmail())));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(currentEmail());
        return ResponseEntity.noContent().build();
    }

    private String currentEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
