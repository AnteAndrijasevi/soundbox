package hr.andrijasevic.soundbox.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverArtImageDto {
    private boolean front;
    private String image;
}
