package hr.andrijasevic.soundbox.dto;

public record ListItemDto(
        String albumMbid,
        String albumTitle,
        String coverArtUrl,
        String artist,
        Integer position,
        String note
) {}
