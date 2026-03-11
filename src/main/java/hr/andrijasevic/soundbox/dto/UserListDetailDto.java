package hr.andrijasevic.soundbox.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserListDetailDto(
        Long id,
        String name,
        String description,
        boolean isPublic,
        String username,
        List<ListItemDto> items,
        LocalDateTime createdAt
) {}
