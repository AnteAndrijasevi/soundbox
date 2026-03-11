package hr.andrijasevic.soundbox.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listen_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListenLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    @Column(nullable = false)
    private LocalDateTime listenedAt;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;
}
