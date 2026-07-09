package hr.andrijasevic.soundbox.external.itunes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ITunesAlbumResult {
    private String collectionName;
    private String artistName;
    private String artworkUrl100;
}
