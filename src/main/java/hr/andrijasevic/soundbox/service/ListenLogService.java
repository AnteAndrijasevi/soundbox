package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListenLog;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.ListenLogDto;
import hr.andrijasevic.soundbox.dto.ListenLogRequest;
import hr.andrijasevic.soundbox.exception.ResourceNotFoundException;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListenLogRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ListenLogService {

    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;
    private final ListenLogRepository listenLogRepository;

    public ListenLogService(
            UserRepository userRepository,
            AlbumRepository albumRepository,
            AlbumService albumService,
            ListenLogRepository listenLogRepository
    ) {
        this.userRepository = userRepository;
        this.albumRepository = albumRepository;
        this.albumService = albumService;
        this.listenLogRepository = listenLogRepository;
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
