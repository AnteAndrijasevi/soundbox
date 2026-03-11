package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByUserIdAndAlbumId(Long userId, Long albumId);

    Page<Review> findByAlbumMbidOrderByCreatedAtDesc(String mbid, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndAlbumId(Long userId, Long albumId);

    int countByUserId(Long userId);

    Page<Review> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);
}
