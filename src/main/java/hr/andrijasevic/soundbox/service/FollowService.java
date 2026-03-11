package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Follow;
import hr.andrijasevic.soundbox.domain.FollowId;
import hr.andrijasevic.soundbox.domain.Review;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.ReviewDto;
import hr.andrijasevic.soundbox.repository.FollowRepository;
import hr.andrijasevic.soundbox.repository.LikeRepository;
import hr.andrijasevic.soundbox.repository.ReviewRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;

    public FollowService(
            FollowRepository followRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            LikeRepository likeRepository
    ) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
    }

    public void toggleFollow(Long targetUserId, String email) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getId().equals(targetUserId)) {
            throw new RuntimeException("Cannot follow yourself");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (followRepository.existsByIdFollowerIdAndIdFollowingId(currentUser.getId(), targetUserId)) {
            followRepository.deleteByIdFollowerIdAndIdFollowingId(currentUser.getId(), targetUserId);
        } else {
            FollowId followId = FollowId.builder()
                    .followerId(currentUser.getId())
                    .followingId(targetUserId)
                    .build();
            Follow follow = Follow.builder()
                    .id(followId)
                    .follower(currentUser)
                    .following(targetUser)
                    .createdAt(LocalDateTime.now())
                    .build();
            followRepository.save(follow);
        }
    }

    public Page<ReviewDto> getFeed(String email, Pageable pageable) {
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Follow> following = followRepository.findByIdFollowerId(currentUser.getId());
        List<Long> followingIds = following.stream()
                .map(f -> f.getId().getFollowingId())
                .toList();

        if (followingIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return reviewRepository.findByUserIdInOrderByCreatedAtDesc(followingIds, pageable)
                .map(this::mapToDto);
    }

    private ReviewDto mapToDto(Review review) {
        int likeCount = likeRepository.countByIdReviewId(review.getId());
        String albumMbid = review.getAlbum() != null ? review.getAlbum().getMbid() : null;
        String albumTitle = review.getAlbum() != null ? review.getAlbum().getTitle() : null;
        String username = review.getUser() != null ? review.getUser().getDisplayUsername() : null;
        return new ReviewDto(
                review.getId(),
                albumMbid,
                albumTitle,
                username,
                review.getRating(),
                review.getText(),
                review.getCreatedAt(),
                likeCount
        );
    }
}
