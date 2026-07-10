package hr.andrijasevic.soundbox.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String actorUsername,
        String albumMbid,
        String albumTitle,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}
