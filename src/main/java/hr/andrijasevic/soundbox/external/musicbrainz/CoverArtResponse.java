package hr.andrijasevic.soundbox.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoverArtResponse {
    private List<CoverArtImageDto> images;
}
