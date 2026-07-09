package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Like;
import hr.andrijasevic.soundbox.domain.Review;
import hr.andrijasevic.soundbox.domain.User;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeRepository likeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private LikeService likeService;

    private User user;
    private Review review;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("ante").email("ante@example.com").passwordHash("x").build();
        review = Review.builder().id(50L).build();
    }

    @Test
    void toggleLike_likesWhenNotLiked() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(likeRepository.existsByIdUserIdAndIdReviewId(1L, 50L)).thenReturn(false);

        likeService.toggleLike(50L, "ante@example.com");

        ArgumentCaptor<Like> captor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository).save(captor.capture());
        assertThat(captor.getValue().getId().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getId().getReviewId()).isEqualTo(50L);
    }

    @Test
    void toggleLike_unlikesWhenAlreadyLiked() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(50L)).thenReturn(Optional.of(review));
        when(likeRepository.existsByIdUserIdAndIdReviewId(1L, 50L)).thenReturn(true);

        likeService.toggleLike(50L, "ante@example.com");

        verify(likeRepository).deleteByIdUserIdAndIdReviewId(1L, 50L);
        verify(likeRepository, never()).save(any());
    }

    @Test
    void toggleLike_rejectsUnknownReview() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.toggleLike(404L, "ante@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Review not found");
    }
}
