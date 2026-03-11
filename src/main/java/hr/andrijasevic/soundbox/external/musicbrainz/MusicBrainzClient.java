package hr.andrijasevic.soundbox.external.musicbrainz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzAlbumResponse;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

@Service
public class MusicBrainzClient {

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

        this.coverArtWebClient = webClientBuilder
                .baseUrl(coverArtArchiveUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept", "application/json")
                .build();
    }

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
