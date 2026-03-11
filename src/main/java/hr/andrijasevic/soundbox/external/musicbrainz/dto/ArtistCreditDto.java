package hr.andrijasevic.soundbox.external.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtistCreditDto {

    private String name;
    private ArtistDto artist;
}
