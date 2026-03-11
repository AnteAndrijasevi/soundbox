package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.Review;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.ReviewDto;
import hr.andrijasevic.soundbox.dto.ReviewRequest;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.LikeRepository;
import hr.andrijasevic.soundbox.repository.ReviewRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ReviewService {

    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;

    public ReviewService(
            UserRepository userRepository,
            AlbumRepository albumRepository,
            AlbumService albumService,
            ReviewRepository reviewRepository,
            LikeRepository likeRepository
    ) {
        this.userRepository = userRepository;
        this.albumRepository = albumRepository;
        this.albumService = albumService;
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
    }

    public ReviewDto createOrUpdateReview(String mbid, ReviewRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<Album> albumOpt = albumRepository.findByMbid(mbid);
        if (albumOpt.isEmpty()) {
            albumService.getAlbum(mbid);
        }
        Album album = albumRepository.findByMbid(mbid)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        Optional<Review> existingReview = reviewRepository.findByUserIdAndAlbumId(user.getId(), album.getId());

        Review review;
        if (existingReview.isPresent()) {
            review = existingReview.get();
            review.setRating(request.rating());
            review.setText(request.text());
            review.setUpdatedAt(LocalDateTime.now());
        } else {
            review = Review.builder()
                    .user(user)
                    .album(album)
                    .rating(request.rating())
                    .text(request.text())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        Review savedReview = reviewRepository.save(review);
        return mapToDto(savedReview);
    }

    public Page<ReviewDto> getReviewsForAlbum(String mbid, Pageable pageable) {
        return reviewRepository.findByAlbumMbidOrderByCreatedAtDesc(mbid, pageable)
                .map(this::mapToDto);
    }

    public Page<ReviewDto> getReviewsForUser(Long userId, Pageable pageable) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToDto);
    }

    public void deleteReview(Long reviewId, String email) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }
        reviewRepository.delete(review);
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
