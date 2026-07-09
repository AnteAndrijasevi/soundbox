package hr.andrijasevic.soundbox.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzAlbumResponse;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzSearchResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import java.util.List;

@Service
public class MusicBrainzClient {

    private static final Logger log = LoggerFactory.getLogger(MusicBrainzClient.class);

    private final WebClient webClient;
    private final WebClient coverArtWebClient;

    public MusicBrainzClient(
            WebClient.Builder webClientBuilder,
            @Value("${musicbrainz.base-url}") String baseUrl,
            @Value("${musicbrainz.user-agent}") String userAgent,
            @Value("${musicbrainz.cover-art-url}") String coverArtArchiveUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept", "application/json")
                .build();

        // Cover Art Archive answers with 307 redirects to the storage host, so the
        // cover-art client must follow redirects (otherwise the body is empty).
        HttpClient redirectingHttpClient = HttpClient.create().followRedirect(true);
        this.coverArtWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(redirectingHttpClient))
                .baseUrl(coverArtArchiveUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @CircuitBreaker(name = "musicbrainz")
    @Retry(name = "musicbrainz", fallbackMethod = "searchAlbumsFallback")
    public MusicBrainzSearchResponse searchAlbums(String query, int limit, int offset) {
        MusicBrainzSearchResponse result = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/release")
                        .queryParam("query", query)
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("fmt", "json")
                        .build())
                .retrieve()
                .bodyToMono(MusicBrainzSearchResponse.class)
                .block();

        return result != null ? result : new MusicBrainzSearchResponse();
    }

    @SuppressWarnings("unused") // resilience4j fallback (invoked reflectively)
    private MusicBrainzSearchResponse searchAlbumsFallback(String query, int limit, int offset, Throwable t) {
        log.warn("MusicBrainz search unavailable for '{}': {}", query, t.toString());
        return new MusicBrainzSearchResponse();
    }

    @CircuitBreaker(name = "musicbrainz")
    @Retry(name = "musicbrainz", fallbackMethod = "getAlbumFallback")
    public MusicBrainzAlbumResponse getAlbum(String mbid) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/release/{mbid}")
                        .queryParam("inc", "artist-credits+genres+media+recordings")
                        .queryParam("fmt", "json")
                        .build(mbid))
                .retrieve()
                .bodyToMono(MusicBrainzAlbumResponse.class)
                .block();
    }

    @SuppressWarnings("unused") // resilience4j fallback (invoked reflectively)
    private MusicBrainzAlbumResponse getAlbumFallback(String mbid, Throwable t) {
        log.warn("MusicBrainz getAlbum unavailable for {}: {}", mbid, t.toString());
        return null;
    }

    public String getCoverArtUrl(String mbid) {
        try {
            CoverArtResponse response = coverArtWebClient.get()
                    .uri("/release/{mbid}", mbid)
                    .retrieve()
                    .bodyToMono(CoverArtResponse.class)
                    .block();

            if (response == null || response.images == null) {
                return null;
            }

            return response.images.stream()
                    .filter(img -> img.front)
                    .map(img -> img.image)
                    .findFirst()
                    .orElse(null);

        } catch (WebClientResponseException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CoverArtResponse {
        public List<CoverArtImage> images;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CoverArtImage {
        public boolean front;
        public String image;
    }
}
