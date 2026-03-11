package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Like;
import hr.andrijasevic.soundbox.domain.LikeId;
import hr.andrijasevic.soundbox.domain.Review;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.repository.LikeRepository;
import hr.andrijasevic.soundbox.repository.ReviewRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public LikeService(
            LikeRepository likeRepository,
            UserRepository userRepository,
            ReviewRepository reviewRepository
    ) {
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    public void toggleLike(Long reviewId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (likeRepository.existsByIdUserIdAndIdReviewId(user.getId(), reviewId)) {
            likeRepository.deleteByIdUserIdAndIdReviewId(user.getId(), reviewId);
        } else {
            LikeId likeId = LikeId.builder()
                    .userId(user.getId())
                    .reviewId(reviewId)
                    .build();
            Like like = Like.builder()
                    .id(likeId)
                    .user(user)
                    .review(review)
                    .createdAt(LocalDateTime.now())
                    .build();
            likeRepository.save(like);
        }
    }
}
