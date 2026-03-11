package hr.andrijasevic.soundbox.dto;

import java.util.List;

public record AlbumDto(
        Long id,
        String mbid,
        String title,
        String artist,
        String releaseDate,
        String coverArtUrl,
        List<String> genres,
        String tracklist
) {}
