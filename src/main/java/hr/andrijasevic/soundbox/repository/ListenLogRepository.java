package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.ListenLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListenLogRepository extends JpaRepository<ListenLog, Long> {

    Page<ListenLog> findByUserIdOrderByListenedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndAlbumId(Long userId, Long albumId);

    int countByUserId(Long userId);
}
