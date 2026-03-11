package hr.andrijasevic.soundbox.external.musicbrainz.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MusicBrainzSearchResponse {

    private List<MusicBrainzReleaseDto> releases;

    @JsonProperty("release-count")
    private int releaseCount;

    @JsonProperty("release-offset")
    private int releaseOffset;
}
