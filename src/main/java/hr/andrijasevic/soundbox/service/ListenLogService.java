package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListenLog;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.ListenLogDto;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListenLogRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public ListenLogDto logListen(String mbid, BigDecimal rating, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Album album = albumRepository.findByMbid(mbid).orElseGet(() -> {
            albumService.getAlbum(mbid);
            return albumRepository.findByMbid(mbid).orElseThrow();
        });

        ListenLog listenLog = new ListenLog();
        listenLog.setUser(user);
        listenLog.setAlbum(album);
        listenLog.setListenedAt(LocalDateTime.now());
        listenLog.setRating(rating);

        ListenLog saved = listenLogRepository.save(listenLog);
        return mapToDto(saved);
    }

    public Page<ListenLogDto> getListenLog(Long userId, Pageable pageable) {
        return listenLogRepository.findByUserIdOrderByListenedAtDesc(userId, pageable).map(this::mapToDto);
    }

    public Page<ListenLogDto> getMyListenLog(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return getListenLog(user.getId(), pageable);
    }

    private ListenLogDto mapToDto(ListenLog log) {
        Album album = log.getAlbum();
        String albumMbid = album != null ? album.getMbid() : null;
        String albumTitle = album != null ? album.getTitle() : null;
        String coverArtUrl = album != null ? album.getCoverArtUrl() : null;
        String artist = album != null && album.getArtist() != null ? album.getArtist().getName() : null;
        return new ListenLogDto(log.getId(), albumMbid, albumTitle, coverArtUrl, artist, log.getListenedAt(), log.getRating());
    }
}
