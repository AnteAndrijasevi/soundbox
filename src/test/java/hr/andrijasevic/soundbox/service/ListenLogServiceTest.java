package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListenContext;
import hr.andrijasevic.soundbox.domain.ListenLog;
import hr.andrijasevic.soundbox.domain.Mood;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.ListenLogDto;
import hr.andrijasevic.soundbox.dto.ListenLogRequest;
import hr.andrijasevic.soundbox.event.ListenEventPublisher;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListenLogRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListenLogServiceTest {

    private static final String MBID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private UserRepository userRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private AlbumService albumService;
    @Mock
    private ListenLogRepository listenLogRepository;
    @Mock
    private ObjectProvider<ListenEventPublisher> eventPublisher;

    @InjectMocks
    private ListenLogService listenLogService;

    private User user;
    private Album album;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("ante").email("ante@example.com").passwordHash("x").build();
        album = Album.builder().id(10L).mbid(MBID).title("OK Computer").build();
    }

    @Test
    void logListen_savesAllContextFields() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(listenLogRepository.save(any(ListenLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ListenLogRequest request = new ListenLogRequest(
                new BigDecimal("4.0"), Mood.NOSTALGIC, ListenContext.NIGHT, true, "2am spin.", "Let Down");

        ListenLogDto dto = listenLogService.logListen(MBID, request, "ante@example.com");

        ArgumentCaptor<ListenLog> captor = ArgumentCaptor.forClass(ListenLog.class);
        verify(listenLogRepository).save(captor.capture());
        ListenLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAlbum()).isEqualTo(album);
        assertThat(saved.getListenedAt()).isNotNull();
        assertThat(saved.getMood()).isEqualTo(Mood.NOSTALGIC);
        assertThat(saved.getContext()).isEqualTo(ListenContext.NIGHT);
        assertThat(saved.getIsFirstListen()).isTrue();
        assertThat(saved.getNote()).isEqualTo("2am spin.");
        assertThat(saved.getFavoriteTrack()).isEqualTo("Let Down");

        assertThat(dto.mood()).isEqualTo(Mood.NOSTALGIC);
        assertThat(dto.context()).isEqualTo(ListenContext.NIGHT);
        assertThat(dto.albumTitle()).isEqualTo("OK Computer");
        verify(albumService, never()).getAlbum(any());
    }

    @Test
    void logListen_acceptsBareLogWithNoContext() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(listenLogRepository.save(any(ListenLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ListenLogDto dto = listenLogService.logListen(
                MBID, new ListenLogRequest(null, null, null, null, null, null), "ante@example.com");

        assertThat(dto.rating()).isNull();
        assertThat(dto.mood()).isNull();
        assertThat(dto.listenedAt()).isNotNull();
    }

    @Test
    void logListen_fetchesAlbumWhenNotCached() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(album));
        when(listenLogRepository.save(any(ListenLog.class))).thenAnswer(inv -> inv.getArgument(0));

        listenLogService.logListen(
                MBID, new ListenLogRequest(null, null, null, null, null, null), "ante@example.com");

        verify(albumService).getAlbum(MBID);
    }

    @Test
    void mapping_prefersItunesArtworkOverCoverArt() {
        album.setArtworkUrl("https://itunes.example/600x600.jpg");
        album.setCoverArtUrl("https://caa.example/front.jpg");
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(listenLogRepository.save(any(ListenLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ListenLogDto dto = listenLogService.logListen(
                MBID, new ListenLogRequest(null, null, null, null, null, null), "ante@example.com");

        assertThat(dto.albumCoverArtUrl()).isEqualTo("https://itunes.example/600x600.jpg");
    }

    @Test
    void mapping_fallsBackToCoverArtWhenNoItunesArtwork() {
        album.setCoverArtUrl("https://caa.example/front.jpg");
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(listenLogRepository.save(any(ListenLog.class))).thenAnswer(inv -> inv.getArgument(0));

        ListenLogDto dto = listenLogService.logListen(
                MBID, new ListenLogRequest(null, null, null, null, null, null), "ante@example.com");

        assertThat(dto.albumCoverArtUrl()).isEqualTo("https://caa.example/front.jpg");
    }

    @Test
    void relistenHistory_returnsUsersLogsForAlbumOldestFirst() {
        ListenLog then = new ListenLog();
        then.setAlbum(album);
        then.setListenedAt(LocalDateTime.now().minusYears(1));
        then.setMood(Mood.MELANCHOLIC);
        ListenLog now = new ListenLog();
        now.setAlbum(album);
        now.setListenedAt(LocalDateTime.now());
        now.setMood(Mood.NOSTALGIC);

        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(listenLogRepository.findByUserIdAndAlbumIdOrderByListenedAtAsc(1L, 10L))
                .thenReturn(List.of(then, now));

        List<ListenLogDto> history = listenLogService.getRelistenHistory(1L, MBID);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).mood()).isEqualTo(Mood.MELANCHOLIC); // "then"
        assertThat(history.get(1).mood()).isEqualTo(Mood.NOSTALGIC);   // "now"
    }

    @Test
    void relistenHistory_returnsEmptyWhenAlbumNeverCached() {
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.empty());

        assertThat(listenLogService.getRelistenHistory(1L, MBID)).isEmpty();
        verifyNoInteractions(listenLogRepository);
    }
}
