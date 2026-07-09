package hr.andrijasevic.soundbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.Artist;
import hr.andrijasevic.soundbox.dto.AlbumDto;
import hr.andrijasevic.soundbox.external.itunes.ITunesClient;
import hr.andrijasevic.soundbox.external.musicbrainz.MusicBrainzClient;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.ArtistCreditDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.ArtistDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.GenreDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzAlbumResponse;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzReleaseDto;
import hr.andrijasevic.soundbox.external.musicbrainz.dto.MusicBrainzSearchResponse;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ArtistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    private static final String MBID = "11111111-1111-1111-1111-111111111111";
    private static final String ARTIST_MBID = "22222222-2222-2222-2222-222222222222";

    @Mock
    private MusicBrainzClient musicBrainzClient;
    @Mock
    private ITunesClient iTunesClient;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private ArtistRepository artistRepository;

    private AlbumService albumService;

    @BeforeEach
    void setUp() {
        albumService = new AlbumService(
                musicBrainzClient, iTunesClient, albumRepository, artistRepository, new ObjectMapper());
    }

    private static MusicBrainzAlbumResponse mbAlbum() {
        ArtistDto artist = new ArtistDto();
        artist.setId(ARTIST_MBID);
        artist.setName("Radiohead");
        ArtistCreditDto credit = new ArtistCreditDto();
        credit.setArtist(artist);
        credit.setName("Radiohead");

        GenreDto genre = new GenreDto();
        genre.setName("art rock");

        MusicBrainzAlbumResponse response = new MusicBrainzAlbumResponse();
        response.setId(MBID);
        response.setTitle("OK Computer");
        response.setDate("1997-06-16");
        response.setArtistCredit(List.of(credit));
        response.setGenres(List.of(genre));
        return response;
    }

    @Test
    void getAlbum_returnsCachedAlbumWithoutHittingExternalApis() {
        Album cached = Album.builder()
                .id(1L)
                .mbid(MBID)
                .title("OK Computer")
                .artist(Artist.builder().mbid(ARTIST_MBID).name("Radiohead").build())
                .artworkUrl("https://itunes.example/art.jpg")
                .lastFetchedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(cached));

        AlbumDto dto = albumService.getAlbum(MBID);

        assertThat(dto.title()).isEqualTo("OK Computer");
        assertThat(dto.artist()).isEqualTo("Radiohead");
        assertThat(dto.artworkUrl()).isEqualTo("https://itunes.example/art.jpg");
        verifyNoInteractions(musicBrainzClient, iTunesClient);
        verify(albumRepository, never()).save(any());
    }

    @Test
    void getAlbum_fetchesAndPersistsWhenNotCached() {
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.empty());
        when(musicBrainzClient.getAlbum(MBID)).thenReturn(mbAlbum());
        when(musicBrainzClient.getCoverArtUrl(MBID)).thenReturn("https://caa.example/front.jpg");
        when(iTunesClient.findArtworkUrl("Radiohead", "OK Computer"))
                .thenReturn("https://itunes.example/600x600.jpg");
        when(artistRepository.findByMbid(ARTIST_MBID)).thenReturn(Optional.empty());
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        AlbumDto dto = albumService.getAlbum(MBID);

        ArgumentCaptor<Album> captor = ArgumentCaptor.forClass(Album.class);
        verify(albumRepository).save(captor.capture());
        Album saved = captor.getValue();
        assertThat(saved.getMbid()).isEqualTo(MBID);
        assertThat(saved.getTitle()).isEqualTo("OK Computer");
        assertThat(saved.getReleaseDate()).isEqualTo(LocalDate.of(1997, 6, 16));
        assertThat(saved.getCoverArtUrl()).isEqualTo("https://caa.example/front.jpg");
        assertThat(saved.getArtworkUrl()).isEqualTo("https://itunes.example/600x600.jpg");
        assertThat(saved.getGenres()).containsExactly("art rock");
        assertThat(saved.getLastFetchedAt()).isNotNull();

        assertThat(dto.artist()).isEqualTo("Radiohead");
        assertThat(dto.artworkUrl()).isEqualTo("https://itunes.example/600x600.jpg");
    }

    @Test
    void getAlbum_refreshesStaleAlbumButKeepsArtworkWhenItunesFindsNothing() {
        Album stale = Album.builder()
                .id(1L)
                .mbid(MBID)
                .title("OK Computer")
                .artworkUrl("https://itunes.example/old-art.jpg")
                .lastFetchedAt(LocalDateTime.now().minusDays(30))
                .build();
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(stale));
        when(musicBrainzClient.getAlbum(MBID)).thenReturn(mbAlbum());
        when(musicBrainzClient.getCoverArtUrl(MBID)).thenReturn(null);
        when(iTunesClient.findArtworkUrl(anyString(), anyString())).thenReturn(null);
        when(artistRepository.findByMbid(ARTIST_MBID))
                .thenReturn(Optional.of(Artist.builder().mbid(ARTIST_MBID).name("Radiohead").build()));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        AlbumDto dto = albumService.getAlbum(MBID);

        verify(musicBrainzClient).getAlbum(MBID);
        assertThat(dto.artworkUrl()).isEqualTo("https://itunes.example/old-art.jpg");
    }

    @Test
    void getAlbum_toleratesUnparseablePartialReleaseDate() {
        MusicBrainzAlbumResponse response = mbAlbum();
        response.setDate("1997-06"); // MusicBrainz partial date
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.empty());
        when(musicBrainzClient.getAlbum(MBID)).thenReturn(response);
        when(artistRepository.findByMbid(ARTIST_MBID))
                .thenReturn(Optional.of(Artist.builder().mbid(ARTIST_MBID).name("Radiohead").build()));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        AlbumDto dto = albumService.getAlbum(MBID);

        assertThat(dto.releaseDate()).isNull();
        assertThat(dto.title()).isEqualTo("OK Computer");
    }

    @Test
    void searchAlbums_mapsReleasesToDtos() {
        ArtistDto artist = new ArtistDto();
        artist.setName("Radiohead");
        ArtistCreditDto credit = new ArtistCreditDto();
        credit.setArtist(artist);

        MusicBrainzReleaseDto release = new MusicBrainzReleaseDto();
        release.setId(MBID);
        release.setTitle("OK Computer");
        release.setDate("1997-06-16");
        release.setArtistCredit(List.of(credit));

        MusicBrainzSearchResponse response = new MusicBrainzSearchResponse();
        response.setReleases(List.of(release));
        when(musicBrainzClient.searchAlbums("ok computer", 10, 0)).thenReturn(response);

        List<AlbumDto> results = albumService.searchAlbums("ok computer", 10, 0);

        assertThat(results).hasSize(1);
        AlbumDto dto = results.get(0);
        assertThat(dto.mbid()).isEqualTo(MBID);
        assertThat(dto.title()).isEqualTo("OK Computer");
        assertThat(dto.artist()).isEqualTo("Radiohead");
        assertThat(dto.id()).isNull(); // search results are never persisted
    }

    @Test
    void searchAlbums_returnsEmptyListWhenNoReleases() {
        when(musicBrainzClient.searchAlbums("nothing", 10, 0)).thenReturn(new MusicBrainzSearchResponse());

        assertThat(albumService.searchAlbums("nothing", 10, 0)).isEmpty();
    }

    @Test
    void searchAlbums_fallsBackToCreditNameWhenArtistMissing() {
        ArtistCreditDto credit = new ArtistCreditDto();
        credit.setName("Various Artists");

        MusicBrainzReleaseDto release = new MusicBrainzReleaseDto();
        release.setId(MBID);
        release.setTitle("Compilation");
        release.setArtistCredit(List.of(credit));

        MusicBrainzSearchResponse response = new MusicBrainzSearchResponse();
        response.setReleases(List.of(release));
        when(musicBrainzClient.searchAlbums("comp", 10, 0)).thenReturn(response);

        assertThat(albumService.searchAlbums("comp", 10, 0).get(0).artist()).isEqualTo("Various Artists");
    }
}
