package hr.andrijasevic.soundbox.external.itunes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.andrijasevic.soundbox.external.itunes.dto.ITunesSearchResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ITunesClient {

    private static final Logger log = LoggerFactory.getLogger(ITunesClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ITunesClient(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${itunes.base-url}") String baseUrl
    ) {
        // iTunes answers with Content-Type text/javascript, so the body is fetched
        // as a String and parsed manually instead of relying on WebClient's JSON codec
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Looks up album artwork via the iTunes Search API. Returns a 600x600 artwork URL,
     * or null if nothing matched. Best-effort: transient failures are retried and, once
     * the circuit trips, degrade to null (see {@link #findArtworkFallback}).
     */
    @CircuitBreaker(name = "itunes")
    @Retry(name = "itunes", fallbackMethod = "findArtworkFallback")
    public String findArtworkUrl(String artistName, String albumTitle) {
        if (albumTitle == null || albumTitle.isBlank()) {
            return null;
        }
        String term = artistName != null && !artistName.isBlank()
                ? artistName + " " + albumTitle
                : albumTitle;

        // network errors from block() propagate to resilience4j (retry / circuit breaker)
        String body = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("term", term)
                        .queryParam("entity", "album")
                        .queryParam("media", "music")
                        .queryParam("limit", 5)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (body == null) {
            return null;
        }
        try {
            ITunesSearchResponse response = objectMapper.readValue(body, ITunesSearchResponse.class);
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return null;
            }
            return response.getResults().stream()
                    .filter(r -> r.getArtworkUrl100() != null)
                    .findFirst()
                    .map(r -> upscale(r.getArtworkUrl100()))
                    .orElse(null);
        } catch (JsonProcessingException e) {
            // a malformed body is not worth retrying — just treat it as "no artwork"
            log.debug("iTunes response parse failed for '{}': {}", term, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unused") // resilience4j fallback (invoked reflectively)
    private String findArtworkFallback(String artistName, String albumTitle, Throwable t) {
        log.warn("iTunes artwork unavailable for '{} - {}': {}", artistName, albumTitle, t.toString());
        return null;
    }

    private String upscale(String artworkUrl100) {
        return artworkUrl100.replace("100x100bb", "600x600bb");
    }
}
