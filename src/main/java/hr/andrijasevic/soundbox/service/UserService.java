package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.dto.UserDto;
import hr.andrijasevic.soundbox.repository.FollowRepository;
import hr.andrijasevic.soundbox.repository.ReviewRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;

    public UserService(
            UserRepository userRepository,
            FollowRepository followRepository,
            ReviewRepository reviewRepository
    ) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.reviewRepository = reviewRepository;
    }

    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        int followerCount = followRepository.countByIdFollowingId(userId);
        int followingCount = followRepository.countByIdFollowerId(userId);
        int reviewCount = reviewRepository.countByUserId(userId);
        return new UserDto(
                user.getId(),
                user.getDisplayUsername(),
                user.getAvatarUrl(),
                user.getBio(),
                followerCount,
                followingCount,
                reviewCount
        );
    }

    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return getProfile(user.getId());
    }

    public UserDto updateBio(String bio, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setBio(bio);
        userRepository.save(user);
        return getProfile(user.getId());
    }
}
