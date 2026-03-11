package hr.andrijasevic.soundbox.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewDto(
        Long id,
        String albumMbid,
        String albumTitle,
        String username,
        BigDecimal rating,
        String text,
        LocalDateTime createdAt,
        int likeCount
) {}
