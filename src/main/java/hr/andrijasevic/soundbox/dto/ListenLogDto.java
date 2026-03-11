package hr.andrijasevic.soundbox.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListenLogDto(
        Long id,
        String albumMbid,
        String albumTitle,
        String albumCoverArtUrl,
        String artist,
        LocalDateTime listenedAt,
        BigDecimal rating
) {}
