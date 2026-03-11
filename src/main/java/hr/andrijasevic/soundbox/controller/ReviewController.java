package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.ReviewDto;
import hr.andrijasevic.soundbox.dto.ReviewRequest;
import hr.andrijasevic.soundbox.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/albums/{mbid}/reviews")
    public ResponseEntity<ReviewDto> createOrUpdateReview(
            @PathVariable String mbid,
            @Valid @RequestBody ReviewRequest request
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(reviewService.createOrUpdateReview(mbid, request, email));
    }

    @GetMapping("/api/albums/{mbid}/reviews")
    public ResponseEntity<Page<ReviewDto>> getReviewsForAlbum(
            @PathVariable String mbid,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsForAlbum(mbid, pageable));
    }

    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        reviewService.deleteReview(reviewId, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/users/{userId}/reviews")
    public ResponseEntity<Page<ReviewDto>> getReviewsForUser(
            @PathVariable Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId, pageable));
    }
}
