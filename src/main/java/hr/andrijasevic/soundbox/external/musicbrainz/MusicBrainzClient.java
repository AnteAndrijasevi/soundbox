package hr.andrijasevic.soundbox.external.musicbrainz;

import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzAlbumResponse;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

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

        HttpClient httpClient = HttpClient.create().followRedirect(true);
        this.coverArtWebClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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

            log.debug("Cover Art Archive response for {}: images={}", mbid,
                    response != null && response.getImages() != null ? response.getImages().size() : "null");

            if (response == null || response.getImages() == null || response.getImages().isEmpty()) {
                return null;
            }

            String url = response.getImages().stream()
                    .filter(CoverArtImageDto::isFront)
                    .map(CoverArtImageDto::getImage)
                    .findFirst()
                    .orElseGet(() -> response.getImages().get(0).getImage());

            log.debug("Cover art URL resolved for {}: {}", mbid, url);
            return url;

        } catch (WebClientResponseException e) {
            log.debug("Cover Art Archive returned {} for mbid={}", e.getStatusCode(), mbid);
            return null;
        } catch (Exception e) {
            log.debug("Cover Art Archive error for mbid={}: {}", mbid, e.getMessage());
            return null;
        }
    }
}
