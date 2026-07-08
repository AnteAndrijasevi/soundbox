package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Follow;
import hr.andrijasevic.soundbox.domain.FollowId;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.repository.FollowRepository;
import hr.andrijasevic.soundbox.repository.LikeRepository;
import hr.andrijasevic.soundbox.repository.ReviewRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private FollowService followService;

    private User ante;
    private User vera;

    @BeforeEach
    void setUp() {
        ante = User.builder().id(1L).username("ante").email("ante@example.com").passwordHash("x").build();
        vera = User.builder().id(2L).username("vera").email("vera@example.com").passwordHash("x").build();
    }

    @Test
    void toggleFollow_followsWhenNotFollowing() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(ante));
        when(userRepository.findById(2L)).thenReturn(Optional.of(vera));
        when(followRepository.existsByIdFollowerIdAndIdFollowingId(1L, 2L)).thenReturn(false);

        followService.toggleFollow(2L, "ante@example.com");

        ArgumentCaptor<Follow> captor = ArgumentCaptor.forClass(Follow.class);
        verify(followRepository).save(captor.capture());
        assertThat(captor.getValue().getId().getFollowerId()).isEqualTo(1L);
        assertThat(captor.getValue().getId().getFollowingId()).isEqualTo(2L);
    }

    @Test
    void toggleFollow_unfollowsWhenAlreadyFollowing() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(ante));
        when(userRepository.findById(2L)).thenReturn(Optional.of(vera));
        when(followRepository.existsByIdFollowerIdAndIdFollowingId(1L, 2L)).thenReturn(true);

        followService.toggleFollow(2L, "ante@example.com");

        verify(followRepository).deleteByIdFollowerIdAndIdFollowingId(1L, 2L);
        verify(followRepository, never()).save(any());
    }

    @Test
    void toggleFollow_rejectsSelfFollow() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(ante));

        assertThatThrownBy(() -> followService.toggleFollow(1L, "ante@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cannot follow yourself");

        verify(followRepository, never()).save(any());
    }

    @Test
    void getFeed_returnsEmptyPageWhenFollowingNobody() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(ante));
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of());

        var feed = followService.getFeed("ante@example.com", PageRequest.of(0, 10));

        assertThat(feed).isEmpty();
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void getFeed_queriesReviewsOfFollowedUsers() {
        Follow follow = Follow.builder()
                .id(FollowId.builder().followerId(1L).followingId(2L).build())
                .follower(ante)
                .following(vera)
                .build();
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(ante));
        when(followRepository.findByIdFollowerId(1L)).thenReturn(List.of(follow));
        when(reviewRepository.findByUserIdInOrderByCreatedAtDesc(List.of(2L), PageRequest.of(0, 10)))
                .thenReturn(org.springframework.data.domain.Page.empty());

        followService.getFeed("ante@example.com", PageRequest.of(0, 10));

        verify(reviewRepository).findByUserIdInOrderByCreatedAtDesc(List.of(2L), PageRequest.of(0, 10));
    }
}
