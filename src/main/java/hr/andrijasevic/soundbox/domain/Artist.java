package hr.andrijasevic.soundbox.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mbid;

    @Column(nullable = false)
    private String name;

    private String bio;

    private String imageUrl;
}
