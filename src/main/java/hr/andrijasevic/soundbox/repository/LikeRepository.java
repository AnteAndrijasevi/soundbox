package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.Like;
import hr.andrijasevic.soundbox.domain.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface LikeRepository extends JpaRepository<Like, LikeId> {

    int countByIdReviewId(Long reviewId);

    boolean existsByIdUserIdAndIdReviewId(Long userId, Long reviewId);

    @Transactional
    void deleteByIdUserIdAndIdReviewId(Long userId, Long reviewId);
}
