package hr.andrijasevic.soundbox.external.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MusicBrainzAlbumResponse {

    private String id;
    private String title;
    private String date;

    @JsonProperty("artist-credit")
    private List<ArtistCreditDto> artistCredit;

    private List<GenreDto> genres;
    private List<MediaDto> media;
}
