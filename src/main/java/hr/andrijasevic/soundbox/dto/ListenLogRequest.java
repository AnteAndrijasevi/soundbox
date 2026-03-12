package hr.andrijasevic.soundbox.dto;

import hr.andrijasevic.soundbox.domain.ListenContext;
import hr.andrijasevic.soundbox.domain.Mood;

import java.math.BigDecimal;

public record ListenLogRequest(
        BigDecimal rating,
        Mood mood,
        ListenContext context,
        Boolean isFirstListen,
        String note,
        String favoriteTrack
) {}