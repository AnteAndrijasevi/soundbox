package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.ListenLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ListenLogRepository extends JpaRepository<ListenLog, Long> {

    Page<ListenLog> findByUserIdOrderByListenedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndAlbumId(Long userId, Long albumId);

    int countByUserId(Long userId);

    /** All of a user's listens of one album, oldest first — the "then → now" timeline. */
    List<ListenLog> findByUserIdAndAlbumIdOrderByListenedAtAsc(Long userId, Long albumId);

    /** A user's listens within a time window, newest first — the "one year ago" nudge. */
    List<ListenLog> findByUserIdAndListenedAtBetweenOrderByListenedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);
}
