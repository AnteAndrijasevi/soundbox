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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final String MBID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private UserRepository userRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private AlbumService albumService;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private LikeRepository likeRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Album album;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("ante").email("ante@example.com").passwordHash("x").build();
        album = Album.builder().id(10L).mbid(MBID).title("OK Computer").build();
    }

    @Test
    void createReview_createsNewReviewForCachedAlbum() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(reviewRepository.findByUserIdAndAlbumId(1L, 10L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDto dto = reviewService.createOrUpdateReview(
                MBID, new ReviewRequest(new BigDecimal("4.5"), "Prophetic."), "ante@example.com");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getAlbum()).isEqualTo(album);
        assertThat(saved.getRating()).isEqualByComparingTo("4.5");
        assertThat(saved.getText()).isEqualTo("Prophetic.");

        assertThat(dto.userId()).isEqualTo(1L);
        assertThat(dto.username()).isEqualTo("ante");
        assertThat(dto.albumMbid()).isEqualTo(MBID);
        verify(albumService, never()).getAlbum(any());
    }

    @Test
    void createReview_fetchesAlbumWhenNotCached() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        // empty on the existence check, present after albumService.getAlbum has cached it
        when(albumRepository.findByMbid(MBID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(album));
        when(reviewRepository.findByUserIdAndAlbumId(1L, 10L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.createOrUpdateReview(
                MBID, new ReviewRequest(new BigDecimal("4.0"), null), "ante@example.com");

        verify(albumService).getAlbum(MBID);
    }

    @Test
    void createReview_updatesExistingReviewInsteadOfDuplicating() {
        Review existing = Review.builder()
                .id(99L)
                .user(user)
                .album(album)
                .rating(new BigDecimal("2.0"))
                .text("meh")
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(reviewRepository.findByUserIdAndAlbumId(1L, 10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewDto dto = reviewService.createOrUpdateReview(
                MBID, new ReviewRequest(new BigDecimal("5.0"), "It grew on me."), "ante@example.com");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(99L); // same row, upserted
        assertThat(saved.getRating()).isEqualByComparingTo("5.0");
        assertThat(saved.getText()).isEqualTo("It grew on me.");
        assertThat(dto.id()).isEqualTo(99L);
    }

    @Test
    void deleteReview_rejectsNonOwner() {
        User other = User.builder().id(2L).username("vera").email("vera@example.com").passwordHash("x").build();
        Review review = Review.builder().id(99L).user(user).album(album).rating(new BigDecimal("4.0")).build();
        when(reviewRepository.findById(99L)).thenReturn(Optional.of(review));
        when(userRepository.findByEmail("vera@example.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> reviewService.deleteReview(99L, "vera@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized");

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReview_deletesOwnReview() {
        Review review = Review.builder().id(99L).user(user).album(album).rating(new BigDecimal("4.0")).build();
        when(reviewRepository.findById(99L)).thenReturn(Optional.of(review));
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(user));

        reviewService.deleteReview(99L, "ante@example.com");

        verify(reviewRepository).delete(review);
    }
}
