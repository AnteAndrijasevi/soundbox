package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListenLog;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.ListenLogDto;
import hr.andrijasevic.soundbox.dto.ListenLogRequest;
import hr.andrijasevic.soundbox.event.ListenEventPublisher;
import hr.andrijasevic.soundbox.event.ListenLoggedEvent;
import hr.andrijasevic.soundbox.exception.ResourceNotFoundException;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListenLogRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ListenLogService {

    // "one year ago" nudge: logs within ±7 days of exactly a year back, capped
    private static final int NUDGE_WINDOW_DAYS = 7;
    private static final int NUDGE_LIMIT = 12;

    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;
    private final ListenLogRepository listenLogRepository;
    private final ObjectProvider<ListenEventPublisher> eventPublisher;

    public ListenLogService(
            UserRepository userRepository,
            AlbumRepository albumRepository,
            AlbumService albumService,
            ListenLogRepository listenLogRepository,
            ObjectProvider<ListenEventPublisher> eventPublisher
    ) {
        this.userRepository = userRepository;
        this.albumRepository = albumRepository;
        this.albumService = albumService;
        this.listenLogRepository = listenLogRepository;
        this.eventPublisher = eventPublisher;
    }


    public Page<ListenLogDto> getListenLog(Long userId, Pageable pageable) {
        return listenLogRepository.findByUserIdOrderByListenedAtDesc(userId, pageable).map(this::mapToDto);
    }

    public Page<ListenLogDto> getMyListenLog(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return getListenLog(user.getId(), pageable);
    }

    /**
     * A user's full listen history for one album, oldest first — the data behind
     * "Then vs Now". Returns an empty list when the album was never cached (and so
     * could never have been logged), rather than treating that as an error.
     */
    public List<ListenLogDto> getRelistenHistory(Long userId, String mbid) {
        return albumRepository.findByMbid(mbid)
                .map(album -> listenLogRepository
                        .findByUserIdAndAlbumIdOrderByListenedAtAsc(userId, album.getId())
                        .stream()
                        .map(this::mapToDto)
                        .toList())
                .orElseGet(List::of);
    }

    public List<ListenLogDto> getMyRelistenHistory(String mbid, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return getRelistenHistory(user.getId(), mbid);
    }

    /**
     * "One year ago" nudges: the albums this user logged around a year ago (±{@value
     * #NUDGE_WINDOW_DAYS} days), deduped to the most recent listen per album, newest first.
     * A gentle retention hook — "revisit what you were playing this time last year".
     */
    public List<ListenLogDto> getRelistenNudges(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LocalDateTime aYearAgo = LocalDateTime.now().minusDays(365);
        LocalDateTime start = aYearAgo.minusDays(NUDGE_WINDOW_DAYS);
        LocalDateTime end = aYearAgo.plusDays(NUDGE_WINDOW_DAYS);

        Map<Long, ListenLogDto> byAlbum = new LinkedHashMap<>();
        for (ListenLog log : listenLogRepository
                .findByUserIdAndListenedAtBetweenOrderByListenedAtDesc(user.getId(), start, end)) {
            if (log.getAlbum() != null) {
                byAlbum.putIfAbsent(log.getAlbum().getId(), mapToDto(log)); // desc order → keeps newest per album
            }
        }
        return byAlbum.values().stream().limit(NUDGE_LIMIT).toList();
    }

    public ListenLogDto logListen(String mbid, ListenLogRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Album album = albumRepository.findByMbid(mbid).orElseGet(() -> {
            albumService.getAlbum(mbid);
            return albumRepository.findByMbid(mbid)
                    .orElseThrow(() -> new ResourceNotFoundException("Album not found"));
        });

        ListenLog listenLog = new ListenLog();
        listenLog.setUser(user);
        listenLog.setAlbum(album);
        listenLog.setListenedAt(LocalDateTime.now());
        listenLog.setRating(request.rating());
        listenLog.setMood(request.mood());
        listenLog.setContext(request.context());
        listenLog.setIsFirstListen(request.isFirstListen());
        listenLog.setNote(request.note());
        listenLog.setFavoriteTrack(request.favoriteTrack());

        ListenLog saved = listenLogRepository.save(listenLog);

        // fan out asynchronously via Kafka (no-op if event publishing is disabled)
        eventPublisher.ifAvailable(publisher -> publisher.publish(new ListenLoggedEvent(
                user.getId(), user.getDisplayUsername(), album.getMbid(), album.getTitle())));

        return mapToDto(saved);
    }

    /** iTunes artwork is higher quality when present; Cover Art Archive is the fallback. */
    private static String bestArtUrl(Album album) {
        return album.getArtworkUrl() != null ? album.getArtworkUrl() : album.getCoverArtUrl();
    }

    private ListenLogDto mapToDto(ListenLog log) {
        Album album = log.getAlbum();
        return new ListenLogDto(
                log.getId(),
                album != null ? album.getMbid() : null,
                album != null ? album.getTitle() : null,
                album != null ? bestArtUrl(album) : null,
                album != null && album.getArtist() != null ? album.getArtist().getName() : null,
                log.getListenedAt(),
                log.getRating(),
                log.getMood(),
                log.getContext(),
                log.getIsFirstListen(),
                log.getNote(),
                log.getFavoriteTrack()
        );
    }
}
