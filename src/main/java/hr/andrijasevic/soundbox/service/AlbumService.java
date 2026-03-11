package hr.andrijasevic.soundbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.Artist;
import hr.andrijasevic.soundbox.dto.AlbumDto;
import hr.andrijasevic.soundbox.external.musicbrainz.MusicBrainzClient;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.ArtistCreditDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.GenreDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzAlbumResponse;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzReleaseDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzSearchResponse;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ArtistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class AlbumService {

    private final MusicBrainzClient musicBrainzClient;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final ObjectMapper objectMapper;

    public AlbumService(
            MusicBrainzClient musicBrainzClient,
            AlbumRepository albumRepository,
            ArtistRepository artistRepository,
            ObjectMapper objectMapper
    ) {
        this.musicBrainzClient = musicBrainzClient;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.objectMapper = objectMapper;
    }

    public List<AlbumDto> searchAlbums(String query, int limit, int offset) {
        MusicBrainzSearchResponse response = musicBrainzClient.searchAlbums(query, limit, offset);
        if (response.getReleases() == null) {
            return List.of();
        }
        return response.getReleases().stream()
                .map(this::mapReleaseToDto)
                .toList();
    }

    private AlbumDto mapReleaseToDto(MusicBrainzReleaseDto release) {
        String artistName = "Unknown Artist";
        if (release.getArtistCredit() != null && !release.getArtistCredit().isEmpty()) {
            ArtistCreditDto credit = release.getArtistCredit().get(0);
            if (credit.getArtist() != null && credit.getArtist().getName() != null) {
                artistName = credit.getArtist().getName();
            } else if (credit.getName() != null) {
                artistName = credit.getName();
            }
        }

        List<String> genres = release.getGenres() != null
                ? release.getGenres().stream()
                        .map(GenreDto::getName)
                        .filter(Objects::nonNull)
                        .toList()
                : List.of();

        String tracklist = null;
        try {
            tracklist = objectMapper.writeValueAsString(release.getMedia());
        } catch (Exception e) {
            // leave null on error
        }

        return new AlbumDto(null, release.getId(), release.getTitle(), artistName, release.getDate(), null, genres, tracklist);
    }

    public AlbumDto getAlbum(String mbid) {
        Optional<Album> existing = albumRepository.findByMbid(mbid);
        if (existing.isPresent()) {
            Album album = existing.get();
            if (album.getLastFetchedAt() != null
                    && album.getLastFetchedAt().isAfter(LocalDateTime.now().minusDays(7))) {
                return mapAlbumEntityToDto(album);
            }
        }

        MusicBrainzAlbumResponse response = musicBrainzClient.getAlbum(mbid);
        String coverArtUrl = musicBrainzClient.getCoverArtUrl(mbid);

        // Resolve artist
        Artist artist = null;
        if (response != null
                && response.getArtistCredit() != null
                && !response.getArtistCredit().isEmpty()) {
            ArtistCreditDto credit = response.getArtistCredit().get(0);
            if (credit.getArtist() != null && credit.getArtist().getId() != null) {
                String artistMbid = credit.getArtist().getId();
                String artistName = credit.getArtist().getName();
                artist = artistRepository.findByMbid(artistMbid).orElseGet(() -> {
                    Artist newArtist = Artist.builder()
                            .mbid(artistMbid)
                            .name(artistName != null ? artistName : "Unknown")
                            .build();
                    return artistRepository.save(newArtist);
                });
            }
        }

        // Resolve genres
        List<String> genres = List.of();
        if (response != null && response.getGenres() != null) {
            genres = response.getGenres().stream()
                    .map(GenreDto::getName)
                    .filter(Objects::nonNull)
                    .toList();
        }

        // Serialize tracklist
        String tracklist = null;
        if (response != null) {
            try {
                tracklist = objectMapper.writeValueAsString(response.getMedia());
            } catch (Exception e) {
                // leave null on error
            }
        }

        // Parse release date
        LocalDate releaseDate = null;
        if (response != null && response.getDate() != null && !response.getDate().isBlank()) {
            try {
                releaseDate = LocalDate.parse(response.getDate());
            } catch (Exception e) {
                // leave null if parsing fails (e.g. partial dates like "2021-03")
            }
        }

        // Create or update Album entity
        Album album = existing.orElseGet(Album::new);
        if (response != null) {
            album.setMbid(response.getId() != null ? response.getId() : mbid);
            album.setTitle(response.getTitle());
        } else {
            album.setMbid(mbid);
        }
        album.setReleaseDate(releaseDate);
        album.setCoverArtUrl(coverArtUrl);
        album.setGenres(genres);
        album.setTracklist(tracklist);
        album.setLastFetchedAt(LocalDateTime.now());
        album.setArtist(artist);

        Album saved = albumRepository.save(album);
        return mapAlbumEntityToDto(saved);
    }

    private AlbumDto mapAlbumEntityToDto(Album album) {
        String artistName = album.getArtist() != null ? album.getArtist().getName() : null;
        return new AlbumDto(
                album.getId(),
                album.getMbid(),
                album.getTitle(),
                artistName,
                album.getReleaseDate() != null ? album.getReleaseDate().toString() : null,
                album.getCoverArtUrl(),
                album.getGenres() != null ? album.getGenres() : List.of(),
                album.getTracklist()
        );
    }
}
