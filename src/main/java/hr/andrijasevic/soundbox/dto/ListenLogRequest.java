package hr.andrijasevic.soundbox.dto;

import hr.andrijasevic.soundbox.domain.ListenContext;
import hr.andrijasevic.soundbox.domain.Mood;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Everything is optional — a bare log is a valid entry — but bounds still apply when present. */
public record ListenLogRequest(
        @DecimalMin("0.5")
        @DecimalMax("5.0")
        BigDecimal rating,

        Mood mood,
        ListenContext context,
        Boolean isFirstListen,

        @Size(max = 500)
        String note,

        @Size(max = 255)
        String favoriteTrack
) {}
