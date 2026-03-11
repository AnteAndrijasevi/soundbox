package hr.andrijasevic.soundbox.dto;

import java.time.LocalDateTime;

public record UserListDto(
        Long id,
        String name,
        String description,
        boolean isPublic,
        String username,
        int itemCount,
        LocalDateTime createdAt
) {}
