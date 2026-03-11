package hr.andrijasevic.soundbox.controller;

import hr.andrijasevic.soundbox.dto.ReviewDto;
import hr.andrijasevic.soundbox.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/users/{userId}/follow")
    public ResponseEntity<Void> toggleFollow(@PathVariable Long userId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        followService.toggleFollow(userId, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<ReviewDto>> getFeed(Pageable pageable) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(followService.getFeed(email, pageable));
    }
}
