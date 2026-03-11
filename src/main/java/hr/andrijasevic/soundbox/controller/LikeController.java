package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class LikeController {

    private final LikeService likeService;

    public LikeController(LikeService likeService) {
        this.likeService = likeService;
    }

    @PostMapping("/{reviewId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long reviewId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        likeService.toggleLike(reviewId, email);
        return ResponseEntity.ok().build();
    }
}
