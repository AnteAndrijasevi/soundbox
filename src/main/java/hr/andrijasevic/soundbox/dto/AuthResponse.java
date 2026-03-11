package hr.andrijasevic.soundbox.dto;

public record AuthResponse(
        String token,
        String username,
        String email
) {}
