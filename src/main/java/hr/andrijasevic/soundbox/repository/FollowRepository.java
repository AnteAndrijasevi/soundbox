package hr.andrijasevic.soundbox.repository;

import hr.andrijasevic.soundbox.domain.Follow;
import hr.andrijasevic.soundbox.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    boolean existsByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);

    @Transactional
    @Modifying
    void deleteByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);

    int countByIdFollowingId(Long followingId);

    int countByIdFollowerId(Long followerId);

    List<Follow> findByIdFollowerId(Long followerId);
}
