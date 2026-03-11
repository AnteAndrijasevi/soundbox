package hr.andrijasevic.soundbox.dto;

public record UserDto(
        Long id,
        String username,
        String avatarUrl,
        String bio,
        int followerCount,
        int followingCount,
        int reviewCount
) {}
