package hr.andrijasevic.soundbox.external.itunes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ITunesSearchResponse {
    private int resultCount;
    private List<ITunesAlbumResult> results;
}
