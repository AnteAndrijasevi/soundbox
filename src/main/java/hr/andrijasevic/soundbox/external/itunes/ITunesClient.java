package hr.andrijasevic.soundbox.external.itunes;

import hr.andrijasevic.soundbox.external.itunes.dto.ITunesSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ITunesClient {

    private static final Logger log = LoggerFactory.getLogger(ITunesClient.class);

    private final WebClient webClient;

    public ITunesClient(
            WebClient.Builder webClientBuilder,
            @Value("${itunes.base-url}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    /**
     * Looks up album artwork via the iTunes Search API. Returns a 600x600 artwork URL,
     * or null if nothing matched. Never throws — artwork is best-effort decoration.
     */
    public String findArtworkUrl(String artistName, String albumTitle) {
        if (albumTitle == null || albumTitle.isBlank()) {
            return null;
        }
        String term = artistName != null && !artistName.isBlank()
                ? artistName + " " + albumTitle
                : albumTitle;
        try {
            ITunesSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("term", term)
                            .queryParam("entity", "album")
                            .queryParam("media", "music")
                            .queryParam("limit", 5)
                            .build())
                    .retrieve()
                    .bodyToMono(ITunesSearchResponse.class)
                    .block();

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return null;
            }

            return response.getResults().stream()
                    .filter(r -> r.getArtworkUrl100() != null)
                    .findFirst()
                    .map(r -> upscale(r.getArtworkUrl100()))
                    .orElse(null);
        } catch (Exception e) {
            log.debug("iTunes artwork lookup failed for '{}': {}", term, e.getMessage());
            return null;
        }
    }

    private String upscale(String artworkUrl100) {
        return artworkUrl100.replace("100x100bb", "600x600bb");
    }
}
